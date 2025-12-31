package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CandidateElectionStatsOfficial;
import Backend.ElectionVote.views.entity.CandidateElectionStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for v_candidate_election_stats_official.
 */
@Repository
public interface CandidateElectionStatsOfficialRepository extends JpaRepository<CandidateElectionStatsOfficial, CandidateElectionStatsOfficialId>,
        JpaSpecificationExecutor<CandidateElectionStatsOfficial> {
}