package Backend.ElectionVote.repository;

import Backend.ElectionVote.views.NecResultGeo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface NecResultGeoRepository
        extends JpaRepository<NecResultGeo, UUID>, JpaSpecificationExecutor<NecResultGeo> {
    List<NecResultGeo> findByElectionId(UUID electionId);
    List<NecResultGeo> findByElectionIdAndCountyId(UUID electionId, UUID countyId);
    List<NecResultGeo> findByElectionIdAndDistrictId(UUID electionId, UUID districtId);
    List<NecResultGeo> findByElectionIdAndCenterId(UUID electionId, UUID centerId);
}

