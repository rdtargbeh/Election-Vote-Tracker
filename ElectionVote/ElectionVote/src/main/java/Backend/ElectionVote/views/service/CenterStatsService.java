package Backend.ElectionVote.views.service;

import Backend.ElectionVote.views.CenterStatsParty;
import Backend.ElectionVote.views.repo.CenterStatsPartyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service exposing read-only center-level stats for a tenant.
 *
 * Behavior:
 *  - Applies tenant GUCs using TenantGucService with SET LOCAL inside the transactional scope.
 *  - Reads the view via the repository with pagination.
 */
@Service
public class CenterStatsService {

    private final CenterStatsPartyRepository repo;
    private final TenantGucService tenantGucService;

    @Autowired
    public CenterStatsService(CenterStatsPartyRepository repo, TenantGucService tenantGucService) {
        this.repo = repo;
        this.tenantGucService = tenantGucService;
    }

    /**
     * Fetch paged center stats for a given org and election.
     * Must set GUCs for RLS to allow the DB to return tenant rows.
     *
     * @param orgId UUID of tenant (as string used for GUC)
     * @param electionId UUID election
     * @param pageable pagination info
     */
    @Transactional(readOnly = true)
    public Page<CenterStatsParty> getCenterStats(UUID orgId, UUID electionId, Pageable pageable) {
        // Apply SET LOCAL app.current_org / app.is_system_admin within this transaction.
        // isSystemAdmin = false for normal tenant calls; if you have a system admin context,
        // pass true instead.
        tenantGucService.applyForTransaction(orgId == null ? "" : orgId.toString(), false);

        // Query the read-only view; RLS will filter rows to the current org.
        return repo.findByIdOrgIdAndIdElectionId(orgId, electionId, pageable);
    }
}