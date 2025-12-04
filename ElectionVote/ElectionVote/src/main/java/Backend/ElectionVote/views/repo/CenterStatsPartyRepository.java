package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.CenterStatsParty;
import Backend.ElectionVote.views.CenterStatsPartyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for the read-only view entity v_center_stats_party.
 *
 * Spring Data can navigate embedded id properties with 'id.<fieldName>' style.
 */
public interface CenterStatsPartyRepository extends JpaRepository<CenterStatsParty, CenterStatsPartyId> {

    // Find all centers for an org+election with pagination.
    Page<CenterStatsParty> findByIdOrgIdAndIdElectionId(UUID orgId, UUID electionId, Pageable pageable);
}