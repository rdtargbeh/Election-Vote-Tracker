package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.District;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DistrictRepository extends JpaRepository<District, UUID> {

    /** For unique (district_name, county_id) guard */
    boolean existsByDistrictNameIgnoreCaseAndCounty_CountyId(String districtName, UUID countyId);

    /** Searching by name within a county */
    Page<District> findByCounty_CountyIdAndDistrictNameContainingIgnoreCase(UUID countyId, String q, Pageable pageable);

    /** Searching by name across all counties */
    Page<District> findByDistrictNameContainingIgnoreCase(String q, Pageable pageable);

    /** List by county without search */
    Page<District> findByCounty_CountyId(UUID countyId, Pageable pageable);
}