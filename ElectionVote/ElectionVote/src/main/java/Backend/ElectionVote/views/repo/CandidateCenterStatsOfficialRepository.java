package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CandidateCenterStatsOfficial;
import Backend.ElectionVote.views.entity.CandidateCenterStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for v_candidate_center_stats_official
 */
@Repository
public interface CandidateCenterStatsOfficialRepository extends JpaRepository<CandidateCenterStatsOfficial, CandidateCenterStatsOfficialId>,
        JpaSpecificationExecutor<CandidateCenterStatsOfficial> {
}