package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.NecResultGeo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NecResultGeoRepository extends JpaRepository<NecResultGeo, UUID> {
    List<NecResultGeo> findByElectionId(UUID electionId);
    List<NecResultGeo> findByElectionIdAndCountyId(UUID electionId, UUID countyId);
    List<NecResultGeo> findByElectionIdAndDistrictId(UUID electionId, UUID districtId);
    List<NecResultGeo> findByElectionIdAndCenterId(UUID electionId, UUID centerId);
}