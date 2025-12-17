package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CandidateElectionStatsParty;
import Backend.ElectionVote.views.entity.CandidateElectionStatsPartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for v_candidate_election_stats_party.
 */
@Repository
public interface CandidateElectionStatsPartyRepository extends JpaRepository<CandidateElectionStatsParty, CandidateElectionStatsPartyId>,
        JpaSpecificationExecutor<CandidateElectionStatsParty> {
}