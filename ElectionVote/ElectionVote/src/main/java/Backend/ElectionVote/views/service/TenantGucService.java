package Backend.ElectionVote.views.service;


import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Sets PostgreSQL session GUCs in a transaction-scoped manner using SET LOCAL.
 *
 * Usage:
 *   - Call applyForTransaction(orgId, isSystemAdmin) from a method that runs inside
 *     a @Transactional context. This registers a TransactionSynchronization that
 *     executes SET LOCAL ... on the transactional JDBC Connection.
 *
 * Rationale:
 *   - SET LOCAL is visible only within the current transaction; it avoids leaking
 *     state across pooled connections.
 */
@Service
public class TenantGucService {

    private final DataSource dataSource;

    public TenantGucService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Apply tenant GUCs for the duration of the current transaction using SET LOCAL.
     * This method must be invoked inside an active transaction (or else it throws).
     *
     * @param orgId tenant id string (empty string to clear)
     * @param isSystemAdmin boolean flag
     */
    public void applyForTransaction(String orgId, boolean isSystemAdmin) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("applyForTransaction must be called inside an active transaction");
        }

        // Register a synchronization that will execute at beforeCommit so the SET LOCAL
        // runs on the same connection the transaction will use for subsequent ops.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                Connection conn = null;
                Statement stmt = null;
                try {
                    // Obtain the transactional connection (DataSourceUtils ensures
                    // we get the same connection bound to the transaction)
                    conn = DataSourceUtils.getConnection(dataSource);
                    stmt = conn.createStatement();

                    // Use SET LOCAL so the setting is scoped to the current transaction.
                    String normalizedOrg = orgId == null ? "" : orgId;
                    String setOrg = "SET LOCAL app.current_org = '" + normalizedOrg + "'";
                    String setAdmin = "SET LOCAL app.is_system_admin = '" + (isSystemAdmin ? "true" : "false") + "'";

                    stmt.execute(setOrg);
                    stmt.execute(setAdmin);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to set tenant GUCs", ex);
                } finally {
                    try { if (stmt != null) stmt.close(); } catch (Exception ignore) {}
                    // Do NOT close the connection manually; DataSourceUtils manages it.
                }
            }
        });
    }
}