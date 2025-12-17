package Backend.ElectionVote.views.repo;

import Backend.ElectionVote.views.entity.CandidateCenterStatsParty;
import Backend.ElectionVote.views.entity.CandidateCenterStatsPartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for v_candidate_center_stats_party
 */
@Repository
public interface CandidateCenterStatsPartyRepository extends JpaRepository<CandidateCenterStatsParty, CandidateCenterStatsPartyId>,
        JpaSpecificationExecutor<CandidateCenterStatsParty> {
}