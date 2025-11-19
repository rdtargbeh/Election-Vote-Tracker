package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.dto.UserUpdateRequest;
import Backend.ElectionVote.entity.*;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.mapper.UserMapper;
import Backend.ElectionVote.repository.*;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.security.CurrentUserProvider;
import Backend.ElectionVote.service.SystemUserService;
import Backend.ElectionVote.utility.ChangePasswordRequest;
import Backend.ElectionVote.utility.QueryUtils;
import Backend.ElectionVote.utility.TenantContext;
import Backend.ElectionVote.utility.UserSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
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
    private final OrgMembershipRepository orgMembershipRepository;
    private final UserMapper mapper; // make this a @Component or MapStruct @Mapper(componentModel="spring")
    private final CurrentUserProvider currentUserProvider;

    private final AuthorizationService authz;

    /* ======================= CREATE ======================= */


    @Override
    @Transactional
    public UserDto createInTenant(UserCreateRequest req) {

        // 1) Determine if caller is platform SYSTEM_ADMIN from TenantContext
        TenantContext ctx = TenantContext.get();
        final boolean callerIsSystemAdmin = (ctx != null && ctx.isSystemAdmin());

        // 2) Resolve current tenant from TenantContext (X-Org-Id header or subdomain)
        UUID orgId = requireTenant();
        Organization tenant = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        // 3) Uniqueness checks
        ensureUniqueEmail(req.getEmail(), null);
        ensureUniqueUsername(req.getUserName(), null);

        // 4) Base platform role
        UserRole role = loadRole(req.getRoleName());

        // Only platform system admin can create ADMIN users
        if (role.getRoleName() == RoleName.ADMIN && !callerIsSystemAdmin) {
            throw new IllegalArgumentException("Only system admin can create ADMIN users");
        }

        // 5) Default organization
        Organization defaultOrg;
        if (req.getDefaultOrgId() != null) {
            defaultOrg = organizationRepository.findById(req.getDefaultOrgId())
                    .orElseThrow(() -> new NoSuchElementException("Default organization not found"));

            if (!callerIsSystemAdmin && !defaultOrg.getOrgId().equals(tenant.getOrgId())) {
                throw new IllegalArgumentException("Org admin cannot assign user to another organization");
            }
        } else {
            defaultOrg = tenant;
        }

        // 6) Neutral user (no party, no county yet)
        String encodedPassword = encoder.encode(req.getPassword());

        SystemUser entity = mapper.toEntity(
                req,
                role,
                null,          // party
                null,          // county
                defaultOrg,
                encodedPassword
        );
        SystemUser saved = systemUserRepository.save(entity);

        // 7) Add user to org_membership for THIS tenant
        ensureMembership(tenant.getOrgId(), saved.getUserId(), role.getRoleName().name());

        return toDto(saved);
    }


//    @Override
//    @Transactional
//    public UserDto createInTenant(UserCreateRequest req) {
//        // 1) Caller must be tenant ADMIN or platform SYSTEM_ADMIN
//        authz.requireAny("ADMIN", "SYSTEM_ADMIN");
//
//        final boolean callerIsSystemAdmin = authz.currentRoles().contains("SYSTEM_ADMIN");
//
//        // 2) Resolve current tenant from TenantContext (X-Org-Id header or subdomain)
//        UUID orgId = requireTenant();
//        Organization tenant = organizationRepository.findById(orgId)
//                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
//        // 3) Uniqueness checks
//        ensureUniqueEmail(req.getEmail(), null);
//        ensureUniqueUsername(req.getUserName(), null);
//
//        // 4) Base platform role
//        UserRole role = loadRole(req.getRoleName());
//
//        if (role.getRoleName() == RoleName.ADMIN && !callerIsSystemAdmin) {
//            throw new IllegalArgumentException("Only system admin can create ADMIN users");
//        }
//        // 5) Default organization
//        Organization defaultOrg;
//        if (req.getDefaultOrgId() != null) {
//            defaultOrg = organizationRepository.findById(req.getDefaultOrgId())
//                    .orElseThrow(() -> new NoSuchElementException("Default organization not found"));
//
//            if (!callerIsSystemAdmin && !defaultOrg.getOrgId().equals(tenant.getOrgId())) {
//                throw new IllegalArgumentException("Org admin cannot assign user to another organization");
//            }
//        } else {
//            defaultOrg = tenant;
//        }
//        // 6) Neutral user (no party, no county yet)
//        String encodedPassword = encoder.encode(req.getPassword());
//
//        SystemUser entity = mapper.toEntity(
//                req,
//                role,
//                null,          // party
//                null,          // county
//                defaultOrg,
//                encodedPassword
//        );
//        SystemUser saved = systemUserRepository.save(entity);
//
//        // 7) HERE: user is added to org_membership for THIS tenant
//        ensureMembership(tenant.getOrgId(), saved.getUserId(), role.getRoleName().name());
//
//        return toDto(saved);
//    }


    @Override
    @Transactional
    public UserDto createTenantMemberRestricted(UserCreateRequest req) {
        // Caller: PARTY_ADMIN/ADMIN/SYSTEM_ADMIN in tenant context
        // Enforce target role whitelist
        Set<RoleName> allowed = Set.of(
                RoleName.AGENT, RoleName.SUPERVISOR, RoleName.DATA_ENTRY,
                RoleName.OBSERVER, RoleName.COORDINATOR, RoleName.AUDITOR
        );
        RoleName target = req.getRoleName();
        if (target == null || !allowed.contains(target)) {
            throw new IllegalArgumentException("Role not allowed for tenant member creation");
        }
        // reuse your createInTenant path (which creates membership)
        return createInTenant(req);
    }


    @Override
    @Transactional
    public UserDto createTenantAdmin(UserCreateRequest req) {
        // Caller: SYSTEM_ADMIN (controller enforced)

        RoleName target = req.getRoleName();
        if (target == null ||
                (target != RoleName.ADMIN && target != RoleName.PARTY_ADMIN)) {
            throw new IllegalArgumentException("Role must be ADMIN or PARTY_ADMIN for this endpoint");
        }

        return createInTenant(req);
    }



    @Override
    @Transactional
    public UserDto createPlatformAdmin(UserCreateRequest req) {
        // Unscoped platform user (no tenant). Allowed only during bootstrap or by controller guard (SYSTEM_ADMIN).
        ensureUniqueEmail(req.getEmail(), null);
        ensureUniqueUsername(req.getUserName(), null);

        // dynamic role from request, fallback to OBSERVER
        UserRole baseRole = (req.getRoleName() != null)
                ? loadRole(req.getRoleName())
                : loadRole(RoleName.OBSERVER);

        String encoded = encoder.encode(req.getPassword());

        // Platform user: no party, county, or default org.
        SystemUser entity = mapper.toEntity(req, baseRole, null, null, null, encoded);

        // Only mark as platform owner if the role is SYSTEM_ADMIN
        entity.setSystemAdmin(baseRole.getRoleName() == RoleName.SYSTEM_ADMIN);
        entity.setVerified(true);
        entity.setActive(true);
        entity.setFailedLoginAttempts(0);
        entity.setLockedUntil(null);
        entity.setLastPasswordChange(LocalDateTime.now());

        SystemUser saved = systemUserRepository.save(entity);

        // NOTE: no OrgMembership is created for platform admins.
        return toDto(saved);
    }



    @Override
    @Transactional
    public UserDto assignUserToCountyAndRole(UUID userId, UUID countyId, String roleName) {
        // 1) Only tenant ADMIN or SYSTEM_ADMIN can do this
        authz.requireAny("ADMIN", "SYSTEM_ADMIN");
        boolean callerIsSystemAdmin = authz.currentRoles().contains("SYSTEM_ADMIN");

        // 2) Current tenant (org)
        UUID orgId = requireTenant();
        Organization tenant = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        // 3) Load user & county
        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        County county = countyRepository.findById(countyId)
                .orElseThrow(() -> new NoSuchElementException("County not found"));

        // 4) Ensure user belongs to this tenant (must have membership)
        OrgMembership membership = orgMembershipRepository
                .findByOrganization_OrgIdAndUser_UserId(tenant.getOrgId(), user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this organization"));

        // 5) Validate roleName (tenant-scoped roles)
        String normalizedRole = roleName == null ? null : roleName.trim().toUpperCase();
        if (normalizedRole == null || normalizedRole.isBlank()) {
            throw new IllegalArgumentException("Role name is required");
        }

        // Only allow tenant-scoped roles here (no SYSTEM_ADMIN via this API)
        Set<String> allowedRoles = Set.of(
                "ADMIN", "PARTY_ADMIN", "AGENT", "OBSERVER", "SUPERVISOR",
                "COORDINATOR", "DATA_ENTRY"
        );

        if (!allowedRoles.contains(normalizedRole)) {
            throw new IllegalArgumentException("Unsupported role for assignment: " + normalizedRole);
        }

        // Optional: prevent non-system-admin from assigning ADMIN at tenant level
        if ("ADMIN".equals(normalizedRole) && !callerIsSystemAdmin) {
            throw new IllegalArgumentException("Only system admin can assign ADMIN role");
        }

        // 6) Apply assignment
        user.setAssignedCounty(county);          // James Doe → Nimba County
        membership.setRoleName(normalizedRole);  // Role in this tenant → COORDINATOR

        // JPA will flush changes at transaction commit, but you can be explicit:
        systemUserRepository.save(user);
        orgMembershipRepository.save(membership);

        // 7) Return updated DTO
        return toDto(user);
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

        if (!encoder.matches(req.getCurrentPassword(), u.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        u.setPassword(encoder.encode(req.getNewPassword()));
        u.setLastPasswordChange(LocalDateTime.now());
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
    }

    @Override
    @Transactional
    public void adminResetPasswordInTenant(UUID userId, String newPassword) {
        UUID orgId = requireTenant();
        SystemUser u = loadTenantUser(orgId, userId);
        u.setPassword(encoder.encode(newPassword));
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


