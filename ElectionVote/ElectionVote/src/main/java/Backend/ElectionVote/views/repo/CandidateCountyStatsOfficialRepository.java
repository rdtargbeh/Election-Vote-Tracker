package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CandidateCountyStatsOfficial;
import Backend.ElectionVote.views.entity.CandidateCountyStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for v_candidate_county_stats_official.
 */
@Repository
public interface CandidateCountyStatsOfficialRepository extends JpaRepository<CandidateCountyStatsOfficial, CandidateCountyStatsOfficialId>,
        JpaSpecificationExecutor<CandidateCountyStatsOfficial> {
}