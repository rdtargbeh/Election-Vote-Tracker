package Backend.ElectionVote.views.repo;


import Backend.ElectionVote.views.entity.CountyStatsOfficial;
import Backend.ElectionVote.views.entity.CountyStatsOfficialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CountyStatsOfficialRepository extends JpaRepository<CountyStatsOfficial, CountyStatsOfficialId>,
        JpaSpecificationExecutor<CountyStatsOfficial> {
}