package Backend.ElectionVote.security;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.repository.OrgMembershipRepository;
import Backend.ElectionVote.uility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private final OrgMembershipRepository memberships;
    private final CurrentUserProvider currentUser;

    @Override
    public OrgMembership requireMembership() {
        TenantContext ctx = TenantContext.get();
        if (ctx == null) throw new AccessDeniedException("Tenant context missing");

        if (ctx.isSystemAdmin()) {
            return OrgMembership.systemAdmin(ctx.userId().orElse(null), ctx.orgId().orElse(null));
        }

        UUID orgId = ctx.orgId().orElseThrow(() -> new AccessDeniedException("Tenant required"));
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
        try { requireAny(roleNames); return true; }
        catch (AccessDeniedException e) { return false; }
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
}