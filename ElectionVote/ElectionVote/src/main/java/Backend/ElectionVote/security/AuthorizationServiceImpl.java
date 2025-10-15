package Backend.ElectionVote.security;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.repository.OrgMembershipRepository;
import Backend.ElectionVote.uility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
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
        UUID orgId  = requireTenant();
        UUID userId = requireUser();

        return memberships.findByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(orgId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this organization or membership disabled"));
    }

    @Override
    public OrgMembership requireAny(String... roleNames) {
        OrgMembership m = requireMembership();
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
        // Single per-org role in this design
        try { return Set.of(requireMembership().getRoleName()); }
        catch (AccessDeniedException e) { return Set.of(); }
    }

    /* ----------------------- helpers ----------------------- */

    private UUID requireTenant() {
        UUID orgId = TenantContext.get();
        if (orgId == null) throw new AccessDeniedException("Tenant required");
        return orgId;
    }

    private UUID requireUser() {
        UUID userId = currentUser.currentUserId();
        if (userId == null) throw new AccessDeniedException("Authentication required");
        return userId;
    }
}