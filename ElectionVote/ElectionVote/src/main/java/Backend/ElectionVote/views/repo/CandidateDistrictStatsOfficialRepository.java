package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CandidateDistrictStatsOfficial;
import Backend.ElectionVote.views.entity.CandidateDistrictStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for v_candidate_district_stats_official.
 */
@Repository
public interface CandidateDistrictStatsOfficialRepository extends JpaRepository<CandidateDistrictStatsOfficial, CandidateDistrictStatsOfficialId>,
        JpaSpecificationExecutor<CandidateDistrictStatsOfficial> {
}