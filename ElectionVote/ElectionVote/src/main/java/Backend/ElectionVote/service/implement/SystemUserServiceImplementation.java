package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.dto.UserUpdateRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.mapper.UserMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.security.CurrentUserProvider;
import Backend.ElectionVote.service.SystemUserService;
import Backend.ElectionVote.uility.ChangePasswordRequest;
import Backend.ElectionVote.uility.QueryUtils;
import Backend.ElectionVote.uility.TenantContext;
import Backend.ElectionVote.uility.UserSearchRequest;
import lombok.RequiredArgsConstructor;
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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemUserServiceImplementation implements SystemUserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES        = 30;

    private final SystemUserRepository systemUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PartyRepository partyRepository;
    private final CountyRepository countyRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository memberships;
    private final PasswordEncoder encoder;
    private final UserMapper mapper; // make this a @Component or MapStruct @Mapper(componentModel="spring")
    private final CurrentUserProvider currentUserProvider;

    /* ======================= CREATE ======================= */

    @Override
    @Transactional
    public UserDto createInTenant(UserCreateRequest req) {
        UUID orgId = requireTenant();

        Organization tenant = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        ensureUniqueEmail(req.getEmail(), null);
        ensureUniqueUsername(req.getUserName(), null);

        UserRole role = loadRole(req.getRoleName());
        if (role.getRoleName() == RoleName.ADMIN && !callerIsPlatformAdmin()) {
            throw new IllegalArgumentException("Not allowed to create ADMIN within a tenant");
        }

        // Party: explicit request value else fallback to tenant party
        Party party = (req.getPartyId() != null)
                ? loadParty(req.getPartyId())
                : tenant.getParty();

        County county = (req.getAssignedCountyId() != null) ? loadCounty(req.getAssignedCountyId()) : null;

        Organization defaultOrg = (req.getDefaultOrgId() != null) ? loadOrg(req.getDefaultOrgId()) : tenant;

        String encoded = encoder.encode(req.getPassword());
        SystemUser entity = mapper.toEntity(req, role, party, county, defaultOrg, encoded);
        SystemUser saved  = systemUserRepository.save(entity);

        ensureMembership(orgId, saved.getUserId(), role.getRoleName().name());

        return toDto(saved);
    }

    /* ======================= READ ======================= */

    @Override
    public Optional<UserDto> getInTenant(UUID userId) {
        UUID orgId = requireTenant();
        if (!callerIsPlatformAdmin()
                && !memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
            return Optional.empty();
        }
        return systemUserRepository.findById(userId).map(this::toDto);
    }

    @Override
    public Page<UserDto> searchInTenant(UserSearchRequest req, Pageable pageable) {
        UUID orgId = requireTenant();
        return systemUserRepository
                .findAllInOrg(
                        orgId,
                        QueryUtils.normalize(req != null ? req.getQ() : null),
                        req != null ? req.getActive() : null,
                        pageable
                )
                .map(this::toDto);
    }

    /* ======================= UPDATE ======================= */

    @Override
    @Transactional
    public UserDto updateInTenant(UUID userId, UserUpdateRequest req) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId); // respects admin bypass

        if (req.getEmail() != null && !u.getEmail().equalsIgnoreCase(req.getEmail())) {
            ensureUniqueEmail(req.getEmail(), u.getUserId());
        }
        if (req.getUserName() != null && !u.getUserName().equalsIgnoreCase(req.getUserName())) {
            ensureUniqueUsername(req.getUserName(), u.getUserId());
        }

        mapper.applyUpdate(req, u);

        if (req.getRoleName() != null) {
            UserRole role = loadRole(req.getRoleName());
            if (role.getRoleName() == RoleName.ADMIN && !callerIsPlatformAdmin()) {
                throw new IllegalArgumentException("Not allowed to assign ADMIN within a tenant");
            }
            u.setRole(role);
            syncMembershipRole(orgId, userId, role.getRoleName().name());
        }

        // NOTE: with current DTO shape, null means “not provided”, so you can set but not clear.
        if (req.getPartyId() != null) {
            u.setParty(loadParty(req.getPartyId()));
        }
        if (req.getAssignedCountyId() != null) {
            u.setAssignedCounty(loadCounty(req.getAssignedCountyId()));
        }
        if (req.getDefaultOrgId() != null) {
            u.setDefaultOrg(loadOrg(req.getDefaultOrgId()));
        }

        return toDto(u);
    }

    /* ======================= FLAGS ======================= */

    @Override
    @Transactional
    public void setActiveInTenant(UUID userId, boolean active) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setActive(active);
    }

    @Override
    @Transactional
    public void setVerifiedInTenant(UUID userId, boolean verified) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setVerified(verified);
        if (verified) {
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
        }
    }

    /* ======================= CREDENTIALS ======================= */

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        // Optional tenant guard if context present and caller isn’t platform admin
        TenantContext ctx = TenantContext.get();
        UUID orgId = (ctx != null) ? ctx.orgId().orElse(null) : null;
        if (orgId != null && !callerIsPlatformAdmin()
                && !memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
            throw new IllegalArgumentException("Not in current tenant");
        }

        SystemUser u = systemUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (!encoder.matches(req.getCurrentPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        u.setPasswordHash(encoder.encode(req.getNewPassword()));
        u.setLastPasswordChange(LocalDateTime.now());
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
    }

    @Override
    @Transactional
    public void adminResetPasswordInTenant(UUID userId, String newPassword) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setPasswordHash(encoder.encode(newPassword));
        u.setLastPasswordChange(LocalDateTime.now());
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
    }

    @Override
    @Transactional
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
    @Transactional
    public void recordLoginFailure(UUID userId) {
        systemUserRepository.findById(userId).ifPresent(u -> {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            if (u.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
        });
    }

    @Override
    @Transactional
    public void recordLoginSuccess(UUID userId) {
        systemUserRepository.findById(userId).ifPresent(u -> {
            u.setLastLogin(LocalDateTime.now());
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
        });
    }

    /* ======================= ROLES & AFFILIATIONS ======================= */

    @Override
    @Transactional
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
    @Transactional
    public void assignPartyInTenant(UUID userId, UUID partyId) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setParty(partyId == null ? null : loadParty(partyId)); // can clear here
    }

    @Override
    @Transactional
    public void assignCountyInTenant(UUID userId, UUID countyId) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setAssignedCounty(countyId == null ? null : loadCounty(countyId));
    }

    @Override
    @Transactional
    public void setDefaultOrgInTenant(UUID userId, UUID orgId) {
        UUID current = requireTenant();
        SystemUser u = loadTenantUser(current, userId);
        if (orgId == null) {
            u.setDefaultOrg(null);
        } else {
            Organization o = loadOrg(orgId);
            u.setDefaultOrg(o);
        }
    }

    /* ======================= HELPERS ======================= */

    private UUID requireTenant() {
        TenantContext c = TenantContext.get();
        UUID orgId = (c != null) ? c.orgId().orElse(null) : null;
        if (orgId == null && !callerIsPlatformAdmin()) {
            throw new IllegalStateException("X-Org-Id is required");
        }
        return orgId; // may be null for platform admin operations
    }

    private SystemUser loadTenantUser(UUID orgId, UUID userId) {
        if (!callerIsPlatformAdmin()) {
            if (orgId == null) throw new IllegalStateException("X-Org-Id is required");
            if (!memberships.existsByOrganization_OrgIdAndUser_UserId(orgId, userId)) {
                throw new IllegalArgumentException("User not in current tenant");
            }
        }
        return systemUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private boolean callerIsPlatformAdmin() {
        UUID me = currentUserProvider.currentUserId();
        if (me == null) return false;
        return systemUserRepository.findById(me)
                .map(SystemUser::isSystemAdmin)
                .orElse(false);
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
                .orElseThrow(() -> new NoSuchElementException("Party not found: " + partyId));
    }

    private County loadCounty(UUID countyId) {
        return countyRepository.findById(countyId)
                .orElseThrow(() -> new NoSuchElementException("County not found: " + countyId));
    }

    private Organization loadOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + orgId));
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
        m.setOrganization(org);
        m.setUser(user);
        m.setRoleName(roleNameText);
        m.setEnabled(true);
        memberships.save(m);
    }

    private void syncMembershipRole(UUID orgId, UUID userId, String roleNameText) {
        memberships.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .ifPresent(m -> m.setRoleName(roleNameText));
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


