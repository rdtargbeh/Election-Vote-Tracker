package Backend.ElectionVote.views.repo;

import Backend.ElectionVote.views.entity.ElectionStatsParty;
import Backend.ElectionVote.views.entity.ElectionStatsPartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for reading v_election_stats_party.
 */
@Repository
public interface ElectionStatsPartyRepository extends JpaRepository<ElectionStatsParty, ElectionStatsPartyId>,
        JpaSpecificationExecutor<ElectionStatsParty> {
}