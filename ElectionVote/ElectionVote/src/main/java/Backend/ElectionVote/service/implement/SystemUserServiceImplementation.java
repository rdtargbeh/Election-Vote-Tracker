package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.dto.UserUpdateRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.mapper.UserMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.service.SystemUserService;
import Backend.ElectionVote.uility.ChangePasswordRequest;
import Backend.ElectionVote.uility.TenantContext;
import Backend.ElectionVote.uility.UserSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SystemUserServiceImplementation implements SystemUserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES        = 30;

    @Autowired
    private  SystemUserRepository systemUserRepository;
    @Autowired
    private  UserRoleRepository userRoleRepository;
    @Autowired
    private  PartyRepository partyRepository;
    @Autowired
    private  CountyRepository countyRepository;
    @Autowired
    private  OrganizationRepository organizationRepository;
    @Autowired
    private  OrgMembershipRepository memberships;
    @Autowired
    private  PasswordEncoder encoder;
    private final UserMapper mapper = new UserMapper();


/* ============================================================
       Core CRUD / Query (Tenant-scoped)
       ============================================================ */


    @Override
    @Transactional
    public UserDto createInTenant(UserCreateRequest req) {
        UUID orgId = requireTenant();

        Organization tenant = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        // global uniqueness per schema
        ensureUniqueEmail(req.getEmail(), null);
        ensureUniqueUsername(req.getUserName(), null);

        // role required & allowed
        UserRole role = loadRole(req.getRoleName());
        if (role.getRoleName() == RoleName.ADMIN && !callerIsPlatformAdmin()) {
            throw new IllegalArgumentException("Not allowed to create ADMIN within a tenant");
        }

        // ------- optional relations -------
        // Party: if not provided, fall back to tenant's linked party (if any)
        Party party = (req.getPartyId() != null)
                ? loadParty(req.getPartyId())
                : (tenant.getParty() != null ? tenant.getParty() : null);

        // County: only if provided
        County county = (req.getAssignedCountyId() != null)
                ? loadCounty(req.getAssignedCountyId())
                : null;

        // Default org: explicit from request, else current tenant
        Organization defaultOrg = (req.getDefaultOrgId() != null)
                ? loadOrg(req.getDefaultOrgId())
                : tenant;

        String encoded = encoder.encode(req.getPassword());

        // NOTE: make sure your mapper has an overload that accepts UserCreateRequest (or rename to CreateUserRequest)
        SystemUser entity = mapper.toEntity(req, role, party, county, defaultOrg, encoded);
        SystemUser saved  = systemUserRepository.save(entity);

        // membership in current tenant (relation-based repo methods)
        ensureMembership(orgId, saved.getUserId(), role.getRoleName().name());

        return toDto(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getInTenant(UUID userId) {
        UUID orgId = requireTenant();
        if (!memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
            return Optional.empty();
        }
        return systemUserRepository.findById(userId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchInTenant(UserSearchRequest req, Pageable pageable) {
        UUID orgId = requireTenant();
        // Simple tenant-scoped search with a join on org_membership
        // Ensure this exists in SystemUserRepository:
        // @Query("select u from SystemUser u where exists (select 1 from OrgMembership m where m.orgId=:org and m.userId=u.userId and m.enabled=true) " +
        //        "and (:q is null or lower(u.firstName) like lower(concat('%',:q,'%')) or lower(u.lastName) like lower(concat('%',:q,'%')) " +
        //        "or lower(u.email) like lower(concat('%',:q,'%')) or lower(u.userName) like lower(concat('%',:q,'%'))) " +
        //        "and (:active is null or u.isActive = :active)")
        // Page<SystemUser> findAllInOrg(@Param("org") UUID orgId, @Param("q") String q, @Param("active") Boolean active, Pageable pg);

        Page<SystemUser> page = systemUserRepository.findAllInOrg(orgId, req.getQ(), req.getActive(), pageable);
        return page.map(this::toDto);
    }

    @Override
    public UserDto updateInTenant(UUID userId, UserUpdateRequest req) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);

        // uniqueness if changed
        if (!u.getEmail().equalsIgnoreCase(req.getEmail())) {
            ensureUniqueEmail(req.getEmail(), u.getUserId());
        }
        if (!u.getUserName().equalsIgnoreCase(req.getUserName())) {
            ensureUniqueUsername(req.getUserName(), u.getUserId());
        }

        // scalar updates
        mapper.applyUpdate(req, u);

        // relations (optional in update request)
        if (req.getRoleName() != null) {
            UserRole role = loadRole(req.getRoleName());
            if (role.getRoleName() == RoleName.ADMIN && !callerIsPlatformAdmin()) {
                throw new IllegalArgumentException("Not allowed to assign ADMIN within a tenant");
            }
            u.setRole(role);
            // keep membership role text in sync for this org if needed
            syncMembershipRole(orgId, userId, role.getRoleName().name());
        }
        if (req.getPartyId() != null) {
            u.setParty(req.getPartyId() == null ? null : loadParty(req.getPartyId()));
        }
        if (req.getAssignedCountyId() != null) {
            u.setAssignedCounty(req.getAssignedCountyId() == null ? null : loadCounty(req.getAssignedCountyId()));
        }
        if (req.getDefaultOrgId() != null) {
            u.setDefaultOrg(req.getDefaultOrgId() == null ? null : loadOrg(req.getDefaultOrgId()));
        }

        return toDto(u);
    }


    /* ============================================================
       Status Flags
       ============================================================ */

    @Override
    public void setActiveInTenant(UUID userId, boolean active) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setActive(active);
    }

    @Override
    public void setVerifiedInTenant(UUID userId, boolean verified) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setVerified(verified);
        if (verified) {
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
        }
    }


    /* ============================================================
       Credentials & Security
       ============================================================ */

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        // user changes their own password (no tenant check necessary if you enforce in controller),
        // but we’ll still guard by membership if TenantContext is present
        UUID orgId = TenantContext.get();
        if (orgId != null && !memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
            throw new IllegalArgumentException("Not in current tenant");
        }

        SystemUser u = systemUserRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!encoder.matches(req.getCurrentPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        u.setPasswordHash(encoder.encode(req.getNewPassword()));
        u.setLastPasswordChange(LocalDateTime.now());
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
    }

    @Override
    public void adminResetPasswordInTenant(UUID userId, String newPassword) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setPasswordHash(encoder.encode(newPassword));
        u.setLastPasswordChange(LocalDateTime.now());
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
    }

    @Override
    public void setLockInTenant(UUID userId, boolean lock, LocalDateTime until) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        if (lock) {
            u.setLockedUntil(until != null ? until : LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        } else {
            u.setLockedUntil(null);
            u.setFailedLoginAttempts(0);
        }
    }

    @Override
    public void recordLoginFailure(UUID userId) {
        // Called by auth flow; no-op if user not found
        systemUserRepository.findById(userId).ifPresent(u -> {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            if (u.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
        });
    }

    @Override
    public void recordLoginSuccess(UUID userId) {
        systemUserRepository.findById(userId).ifPresent(u -> {
            u.setLastLogin(LocalDateTime.now());
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
        });
    }


    /* ============================================================
       Role & Affiliations
       ============================================================ */

    @Override
    public void assignRoleInTenant(UUID userId, RoleName roleName) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        UserRole role = loadRole(roleName);
        if (role.getRoleName() == RoleName.ADMIN && !callerIsPlatformAdmin()) {
            throw new IllegalArgumentException("Not allowed to assign ADMIN within a tenant");
        }
        u.setRole(role);
        syncMembershipRole(orgId, userId, role.getRoleName().name());
    }

    @Override
    public void assignPartyInTenant(UUID userId, UUID partyId) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setParty(partyId == null ? null : loadParty(partyId));
    }

    @Override
    public void assignCountyInTenant(UUID userId, UUID countyId) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setAssignedCounty(countyId == null ? null : loadCounty(countyId));
    }

    @Override
    public void setDefaultOrgInTenant(UUID userId, UUID orgId) {
        UUID current = requireTenant();
        SystemUser u = loadTenantUser(current, userId);
        if (orgId == null) {
            u.setDefaultOrg(null);
        } else {
            Organization o = loadOrg(orgId);
            // optional safety: ensure the user is member of that org too
            u.setDefaultOrg(o);
        }
    }


    /* ============================================================
       Helpers
       ============================================================ */

    private UUID requireTenant() {
        UUID orgId = TenantContext.get();
        if (orgId == null) throw new IllegalStateException("X-Org-Id is required");
        return orgId;
    }

    private SystemUser loadTenantUser(UUID orgId, UUID userId) {
        if (!memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
            throw new IllegalArgumentException("User not in current tenant");
        }
        return systemUserRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private void ensureUniqueEmail(String email, UUID excludeUserId) {
        systemUserRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (excludeUserId == null || !existing.getUserId().equals(excludeUserId)) {
                throw new IllegalArgumentException("Email already in use");
            }
        });
    }

    private void ensureUniqueUsername(String userName, UUID excludeUserId) {
        systemUserRepository.findByUserNameIgnoreCase(userName).ifPresent(existing -> {
            if (excludeUserId == null || !existing.getUserId().equals(excludeUserId)) {
                throw new IllegalArgumentException("Username already in use");
            }
        });
    }

    private UserRole loadRole(RoleName roleName) {
        return userRoleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));
    }

    private Party loadParty(UUID partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new NoSuchElementException("Party not found"));
    }

    private County loadCounty(UUID countyId) {
        return countyRepository.findById(countyId)
                .orElseThrow(() -> new NoSuchElementException("County not found"));
    }

    private Organization loadOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
    }

    private void ensureMembership(UUID orgId, UUID userId, String roleNameText) {
        if (memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
            syncMembershipRole(orgId, userId, roleNameText);
            return;
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        OrgMembership m = new OrgMembership();
        m.setOrganization(org);         // set relations, not IDs
        m.setUser(user);
        m.setRoleName(roleNameText);
        m.setEnabled(true);
        memberships.save(m);
    }


    private void syncMembershipRole(UUID orgId, UUID userId, String roleNameText) {
        memberships.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .ifPresent(m -> m.setRoleName(roleNameText));
    }


    private boolean callerIsPlatformAdmin() {
        // TODO: integrate Spring Security authorities
        return false;
    }

    private UserDto toDto(SystemUser user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setActive(user.isActive());
        dto.setVerified(user.isVerified());
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName().name() : null);
        dto.setPartyId(user.getParty() != null ? user.getParty().getPartyId() : null);
        dto.setAssignedCountyId(user.getAssignedCounty() != null ? user.getAssignedCounty().getCountyId() : null);
        dto.setDefaultOrgId(user.getDefaultOrg() != null ? user.getDefaultOrg().getOrgId() : null);
        dto.setLastLogin(user.getLastLogin());
        dto.setDateCreated(user.getDateCreated());
        return dto;
    }
}
