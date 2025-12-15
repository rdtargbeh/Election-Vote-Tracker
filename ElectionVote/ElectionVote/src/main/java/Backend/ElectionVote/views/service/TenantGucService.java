package Backend.ElectionVote.views.service;


import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

/**
 * Sets PostgreSQL transaction-local GUCs used by RLS and views.
 *
 * Usage:
 *  - MUST be called inside an active Spring-managed transaction (e.g. at the top of methods annotated with @Transactional).
 *  - Call applyForTransaction(...) before any repository/DAO calls in that transaction that depend on RLS or current_setting('app.current_org', ...).
 *
 * Notes:
 *  - Uses SELECT set_config(..., true) which is transaction-local (equivalent to SET LOCAL).
 *  - Uses a PreparedStatement to avoid SQL concatenation/escaping issues.
 *  - Does NOT close the Connection obtained from DataSourceUtils (Spring manages transactional connection lifecycle).
 */

@Service
public class TenantGucService {

    private final DataSource dataSource;

    public TenantGucService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Apply tenant GUCs immediately for the current transaction.
     *
     * @param orgIdStr       organization id as string (empty string to clear). If null, it will be set to empty string.
     * @param isSystemAdmin  true if caller should be treated as system admin (bypass tenant checks)
     * @param isNecAdmin     true if caller should be treated as NEC admin (additional bypass used by some policies)
     */
    public void applyForTransaction(String orgIdStr, boolean isSystemAdmin, boolean isNecAdmin) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("applyForTransaction must be called inside an active transaction");
        }

        final String orgValue = orgIdStr == null ? "" : orgIdStr;
        final String sysAdminValue = isSystemAdmin ? "true" : "false";
        final String necAdminValue = isNecAdmin ? "true" : "false";

        Connection conn = DataSourceUtils.getConnection(dataSource);
        PreparedStatement ps = null;
        try {
            // set_config(..., true) sets a transaction-local setting (equivalent to SET LOCAL)
            ps = conn.prepareStatement(
                    "SELECT set_config('app.current_org', ?, true), " +
                            "set_config('app.is_system_admin', ?, true), " +
                            "set_config('app.is_nec_admin', ?, true)"
            );
            ps.setString(1, orgValue);
            ps.setString(2, sysAdminValue);
            ps.setString(3, necAdminValue);
            ps.execute();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to set tenant GUCs for transaction", ex);
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            // Do NOT close the Connection; DataSourceUtils manages it within the transaction.
        }
    }

    /**
     * Convenience overload: accept orgId as UUID.
     */
    public void applyForTransaction(UUID orgId, boolean isSystemAdmin, boolean isNecAdmin) {
        applyForTransaction(orgId == null ? null : orgId.toString(), isSystemAdmin, isNecAdmin);
    }




//    /**
//     * Apply tenant GUCs for the duration of the current transaction using SET LOCAL.
//     * This method must be invoked inside an active transaction (or else it throws).
//     *
//     * @param orgId tenant id string (empty string to clear)
//     * @param isSystemAdmin boolean flag
//     */
//    public void applyForTransaction(String orgId, boolean isSystemAdmin) {
//        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
//            throw new IllegalStateException("applyForTransaction must be called inside an active transaction");
//        }
//
//        // Register a synchronization that will execute at beforeCommit so the SET LOCAL
//        // runs on the same connection the transaction will use for subsequent ops.
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//
//            @Override
//            public void beforeCommit(boolean readOnly) {
//                Connection conn = null;
//                Statement stmt = null;
//                try {
//                    // Obtain the transactional connection (DataSourceUtils ensures
//                    // we get the same connection bound to the transaction)
//                    conn = DataSourceUtils.getConnection(dataSource);
//                    stmt = conn.createStatement();
//
//                    // Use SET LOCAL so the setting is scoped to the current transaction.
//                    String normalizedOrg = orgId == null ? "" : orgId;
//                    String setOrg = "SET LOCAL app.current_org = '" + normalizedOrg + "'";
//                    String setAdmin = "SET LOCAL app.is_system_admin = '" + (isSystemAdmin ? "true" : "false") + "'";
//
//                    stmt.execute(setOrg);
//                    stmt.execute(setAdmin);
//                } catch (Exception ex) {
//                    throw new RuntimeException("Failed to set tenant GUCs", ex);
//                } finally {
//                    try { if (stmt != null) stmt.close(); } catch (Exception ignore) {}
//                    // Do NOT close the connection manually; DataSourceUtils manages it.
//                }
//            }
//        });
//    }
}