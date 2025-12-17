package Backend.ElectionVote.views.repo;

import Backend.ElectionVote.views.entity.CountyStatsParty;
import Backend.ElectionVote.views.entity.CountyStatsPartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for reading v_county_stats_party.
 */
@Repository
public interface CountyStatsPartyRepository extends JpaRepository<CountyStatsParty, CountyStatsPartyId>,
        JpaSpecificationExecutor<CountyStatsParty> {
}