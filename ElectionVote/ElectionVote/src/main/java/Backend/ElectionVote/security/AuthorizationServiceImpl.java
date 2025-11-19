package Backend.ElectionVote.security;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.repository.OrgMembershipRepository;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.utility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


/**
 * Production implementation:
 *  - Reads the current tenant from TenantContext (populated by TenantFilter)
 *  - Reads current user id from CurrentUserProvider (SecurityContext/JWT)
 *  - Verifies membership is enabled for this tenant
 *  - Performs role checks (case-insensitive)
 *
 * Bean name is "authz" so you can use it from SpEL:
 *   @PreAuthorize("@authz.hasAny('ADMIN','MODERATOR')")
 */
@Service("authz")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    @Autowired
    private OrgMembershipRepository memberships;
    @Autowired
    private CurrentUserProvider currentUser;
    @Autowired
    private SystemUserRepository systemUserRepository;


    @Override
    public OrgMembership requireMembership() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null) {
            throw new AccessDeniedException("Tenant context missing");
        }

        if (ctx.isSystemAdmin()) {
            // Synthetic membership for global admins so callers can proceed uniformly.
            return OrgMembership.systemAdmin(ctx.userId().orElse(null), ctx.orgId().orElse(null));
        }

        UUID orgId = ctx.orgId()
                .orElseThrow(() -> new AccessDeniedException("Tenant required"));
        UUID userId = Optional.ofNullable(currentUser.currentUserId())
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));

        return memberships.findByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(orgId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this organization or membership disabled"));
    }


    @Override
    public OrgMembership requireAny(String... roleNames) {
        OrgMembership m = requireMembership();
        if (m.isSystemAdmin()) return m;
        if (roleNames == null || roleNames.length == 0) return m;

        String have = m.getRoleName();
        for (String want : roleNames) {
            if (want != null && want.equalsIgnoreCase(have)) return m;
        }
        throw new AccessDeniedException("Insufficient role: requires any of " + Arrays.toString(roleNames));
    }

    @Override
    public boolean hasAny(String... roleNames) {
        try {
            requireAny(roleNames);
            return true;
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    @Override
    public Set<String> currentRoles() {
        try {
            OrgMembership m = requireMembership();
            return m.isSystemAdmin() ? Set.of("SYSTEM_ADMIN") : Set.of(m.getRoleName());
        } catch (AccessDeniedException e) {
            return Set.of();
        }
    }

// 🔹 NEW: platform-level admin guard (no tenant required)
@Override
public void requirePlatformAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // 1) Must be authenticated
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
        throw new AuthenticationCredentialsNotFoundException("Authentication required");
    }

    boolean isAdmin = false;

    Object principal = auth.getPrincipal();

    // ---- Case 1: JwtAuthenticationToken (most common with resource server) ----
    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
        Jwt jwt = jwtAuth.getToken();

        // (a) Prefer the JWT claim: isSystemAdmin
        Object claimVal = jwt.getClaims().get("isSystemAdmin");
        if (claimVal instanceof Boolean b) {
            isAdmin = b;
        } else if (claimVal instanceof String s) {
            isAdmin = Boolean.parseBoolean(s);
        }

        // (b) Fallback: check authorities that end with SYSTEM_ADMIN (ROLE_SYSTEM_ADMIN, SYSTEM_ADMIN, etc.)
        if (!isAdmin) {
            isAdmin = jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a != null && a.toUpperCase().endsWith("SYSTEM_ADMIN"));
        }
    }

    // ---- Case 2: principal itself is a Jwt ----
    else if (principal instanceof Jwt jwt) {
        Object claimVal = jwt.getClaims().get("isSystemAdmin");
        if (claimVal instanceof Boolean b) {
            isAdmin = b;
        } else if (claimVal instanceof String s) {
            isAdmin = Boolean.parseBoolean(s);
        }

        if (!isAdmin) {
            isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a != null && a.toUpperCase().endsWith("SYSTEM_ADMIN"));
        }
    }

    // ---- Case 3: Anything else (local dev, username/password, etc.) ----
    else {
        isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a != null && a.toUpperCase().endsWith("SYSTEM_ADMIN"));
    }

    if (!isAdmin) {
        throw new AccessDeniedException("Platform admin required");
    }
}


    /* ---------------- helpers ---------------- */

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean equalsIgnoreCaseNonNull(String a, String b) {
        return b != null && a.equalsIgnoreCase(b);
    }
}
