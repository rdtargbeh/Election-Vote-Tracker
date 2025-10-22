package Backend.ElectionVote.uility;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<TenantContext> CTX = new ThreadLocal<>();

    private final UUID userId;         // nullable
    private final UUID orgId;          // nullable
    private final boolean systemAdmin;

    private TenantContext(UUID userId, UUID orgId, boolean systemAdmin) {
        this.userId = userId;
        this.orgId = orgId;
        this.systemAdmin = systemAdmin;
    }

    public static void set(UUID userId, UUID orgId, boolean systemAdmin) {
        CTX.set(new TenantContext(userId, orgId, systemAdmin));
    }

    public static TenantContext get() { return CTX.get(); }
    public static void clear() { CTX.remove(); }

    public Optional<UUID> userId() { return Optional.ofNullable(userId); }
    public Optional<UUID> orgId() { return Optional.ofNullable(orgId); }
    public boolean isSystemAdmin() { return systemAdmin; }

}

