package Backend.ElectionVote.views;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NecResultGeo
 * --------------------------------------------------------------------
 * READ-ONLY JPA entity mapped to the materialized view:
 *
 *      public.mv_nec_result_geo
 *
 * This materialized view is generated from v_nec_result_geo and contains:
 *  - Official NEC results
 *  - Fully joined geography (center → district → county)
 *  - Candidate vote JSON for each center
 *  - Non-tenant, nationwide results
 *
 * Purpose:
 *  - Fast frontend queries for NEC official results
 *  - Geographic dashboards, maps, charts
 *  - Public / publishable election results (when is_published = true)
 *
 * IMPORTANT:
 *  • This entity MUST NOT be inserted/updated/deleted by JPA.
 *  • It is marked @Immutable to enforce read-only behavior.
 *  • mv_nec_result_geo must be refreshed manually after NEC uploads:
 *        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_nec_result_geo;
 */
@Entity
@Table(
        name = "mv_nec_result_geo",    // <-- materialized view
        indexes = {
                @Index(name = "idx_geo_election", columnList = "election_id"),
                @Index(name = "idx_geo_county",   columnList = "county_id"),
                @Index(name = "idx_geo_district", columnList = "district_id"),
                @Index(name = "idx_geo_center",   columnList = "center_id"),
                @Index(name = "idx_geo_upload_time", columnList = "upload_time")
        }
)
@Immutable // Hibernate must treat this entity as read-only
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NecResultGeo {

    /**
     * Primary key for NEC results (UUID generated when NEC uploads results).
     */
    @Id
    @Column(name = "result_id", nullable = false, updatable = false)
    private UUID resultId;

    /**
     * Election these results belong to.
     * NEC results are NOT tenant-scoped; this is the global election.
     */
    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    /**
     * Polling center where these votes come from.
     */
    @Column(name = "center_id", nullable = false)
    private UUID centerId;
    @Column(name = "center_code")
    private String centerCode;
    @Column(name = "center_name")
    private String centerName;


    /**
     * District information joined via polling_center → district.
     */
    @Column(name = "district_id")
    private UUID districtId;
    @Column(name = "district_name")
    private String districtName;


    /**
     * County information joined via district → county.
     */
    @Column(name = "county_id")
    private UUID countyId;
    @Column(name = "county_name")
    private String countyName;


    /**
     * Candidate votes stored as JSONB:
     *   { "candidateId1": 450, "candidateId2": 120, ... }
     *
     * Parsing happens at the query layer or service layer.
     */
    @Column(name = "candidate_votes", columnDefinition = "jsonb", nullable = false)
    private String candidateVotes;

    /**
     * Registered voters for the center.
     */
    @Column(name = "total_registered_voters", nullable = false)
    private Integer totalRegisteredVoters;

    /**
     * Total ballots cast (valid + invalid).
     */
    @Column(name = "ballots_cast", nullable = false)
    private Integer ballotsCast;

    /**
     * Invalid ballot components.
     */
    @Column(name = "invalid_ballots", nullable = false)
    private Integer invalidBallots;

    @Column(name = "blank_ballots", nullable = false)
    private Integer blankBallots;

    @Column(name = "rejected_ballots", nullable = false)
    private Integer rejectedBallots;

    @Column(name = "spoiled_ballots", nullable = false)
    private Integer spoiledBallots;

    /**
     * Ballots issued (if available from NEC data).
     */
    @Column(name = "ballots_issued")
    private Integer ballotsIssued;

    /**
     * Optional metadata for NEC result ingestion:
     *  - "SMS", "Manual Upload", "API Sync", etc.
     */
    @Column(name = "source", nullable = false)
    private String source;

    /**
     * Timestamp when NEC uploaded these results.
     */
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
}