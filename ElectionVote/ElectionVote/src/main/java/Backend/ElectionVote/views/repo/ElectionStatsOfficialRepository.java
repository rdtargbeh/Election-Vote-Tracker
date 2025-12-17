package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.ElectionStatsOfficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for v_election_stats_official.
 */
@Repository
public interface ElectionStatsOfficialRepository extends JpaRepository<ElectionStatsOfficial, UUID>,
        JpaSpecificationExecutor<ElectionStatsOfficial> {
}