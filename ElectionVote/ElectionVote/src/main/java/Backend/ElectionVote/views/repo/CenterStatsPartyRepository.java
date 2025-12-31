package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CenterStatsParty;
import Backend.ElectionVote.views.entity.CenterStatsPartyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository for the read-only view entity v_center_stats_party.
 *
 * Spring Data can navigate embedded id properties with 'id.<fieldName>' style.
 */
public interface CenterStatsPartyRepository extends JpaRepository<CenterStatsParty, CenterStatsPartyId>,
        JpaSpecificationExecutor<CenterStatsParty> {

    // Find all centers for an org+election with pagination.
    Page<CenterStatsParty> findByIdOrgIdAndIdElectionId(UUID orgId, UUID electionId, Pageable pageable);
}