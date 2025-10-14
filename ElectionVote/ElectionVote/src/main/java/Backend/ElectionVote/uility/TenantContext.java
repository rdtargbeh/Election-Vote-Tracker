package Backend.ElectionVote.uility;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_ORG = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID orgId) { CURRENT_ORG.set(orgId); }

    public static UUID get() { return CURRENT_ORG.get(); }

    public static void clear() { CURRENT_ORG.remove(); }
}
