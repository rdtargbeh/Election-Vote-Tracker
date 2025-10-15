package Backend.ElectionVote.security;

import Backend.ElectionVote.entity.OrgMembership;

import java.util.Set;

public interface AuthorizationService {
    OrgMembership requireMembership();                  // current user in current tenant, enabled
    OrgMembership requireAny(String... roleNames);      // membership + has any of roles
    boolean hasAny(String... roleNames);                // check only, no exception
    Set<String> currentRoles();                         // convenience
}