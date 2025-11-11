package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nec_result_geo",
        indexes = {
                @Index(name = "idx_geo_election", columnList = "election_id"),
                @Index(name = "idx_geo_county", columnList = "county_id"),
                @Index(name = "idx_geo_district", columnList = "district_id"),
                @Index(name = "idx_geo_center", columnList = "center_id"),
                @Index(name = "idx_geo_upload_time", columnList = "upload_time")
        })

@Immutable                         // read-only; Hibernate won’t try INSERT/UPDATE

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NecResultGeo {

    @Id
    @Column(name = "result_id", nullable = false, updatable = false)
    private UUID resultId;

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "center_code")
    private String centerCode;

    @Column(name = "center_name")
    private String centerName;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "county_id")
    private UUID countyId;

    @Column(name = "county_name")
    private String countyName;

    // Store JSONB as String for simplicity. (You can switch to JsonType/JdbcTypeCode later.)
    @Column(name = "candidate_votes", columnDefinition = "jsonb", nullable = false)
    private String candidateVotes;

    @Column(name = "total_registered_voters", nullable = false)
    private Integer totalRegisteredVoters;

    @Column(name = "ballots_cast", nullable = false)
    private Integer ballotsCast;

    @Column(name = "invalid_ballots", nullable = false)
    private Integer invalidBallots;

    @Column(name = "blank_ballots", nullable = false)
    private Integer blankBallots;

    @Column(name = "rejected_ballots", nullable = false)
    private Integer rejectedBallots;

    @Column(name = "spoiled_ballots", nullable = false)
    private Integer spoiledBallots;

    @Column(name = "ballots_issued")
    private Integer ballotsIssued;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
}