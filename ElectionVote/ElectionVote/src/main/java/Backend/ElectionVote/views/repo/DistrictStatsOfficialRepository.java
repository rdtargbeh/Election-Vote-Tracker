package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.DistrictStatsOfficial;
import Backend.ElectionVote.views.entity.DistrictStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DistrictStatsOfficialRepository extends JpaRepository<DistrictStatsOfficial, DistrictStatsOfficialId>,
        JpaSpecificationExecutor<DistrictStatsOfficial> {
}