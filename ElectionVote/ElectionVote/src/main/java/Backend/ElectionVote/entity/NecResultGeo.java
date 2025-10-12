package Backend.ElectionVote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "v_nec_result_geo")   // <-- the SQL VIEW name
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

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
}