package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CenterStatsOfficial;
import Backend.ElectionVote.views.entity.CenterStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CenterStatsOfficialRepository extends JpaRepository<CenterStatsOfficial, CenterStatsOfficialId>,
        JpaSpecificationExecutor<CenterStatsOfficial> {
}