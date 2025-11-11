package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ObserverReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;


@Repository
public interface ObserverReportRepository
        extends JpaRepository<ObserverReport, UUID>, JpaSpecificationExecutor<ObserverReport> {

    // Optional: spatial search using PostGIS ST_DWithin on geography
    @Query(value = """
      SELECT * FROM observer_report r
      WHERE (:orgId IS NULL OR r.org_id = :orgId)
        AND ST_DWithin(r.gps_location, ST_SetSRID(ST_MakePoint(:lon,:lat),4326)::geography, :meters)
      ORDER BY r.timestamp DESC
      """,
            nativeQuery = true)
    List<ObserverReport> findNear(@Param("orgId") UUID orgId,
                                  @Param("lon") double lon,
                                  @Param("lat") double lat,
                                  @Param("meters") double meters);
}