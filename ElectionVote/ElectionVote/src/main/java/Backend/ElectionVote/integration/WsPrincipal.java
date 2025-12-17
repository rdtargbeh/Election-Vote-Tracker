package Backend.ElectionVote.integration;


import java.security.Principal;
import java.util.UUID;

public record WsPrincipal(UUID userId, UUID orgId) implements Principal {
    @Override public String getName() { return userId.toString(); }
}

