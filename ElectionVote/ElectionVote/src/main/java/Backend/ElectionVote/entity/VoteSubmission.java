package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.VoteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "vote_submission")
public class VoteSubmission extends AuditBaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private PollingCenter pollingCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vote_submission_place"))
    private PollingPlace pollingPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private SystemUser agent;

    @Column(name = "submission_time")
    private LocalDateTime submissionTime = LocalDateTime.now();

    @Column(name = "candidate_votes", columnDefinition = "jsonb", nullable = false)
    private String candidateVotes;

    @Column(name = "ballots_cast", nullable = false)
    private Integer ballotsCast;

    @Column(name = "invalid_ballots", nullable = false)
    private Integer invalidBallots = 0;

    @Column(name = "blank_ballots", nullable = false)
    private Integer blankBallots = 0;

    @Column(name = "rejected_ballots", nullable = false)
    private Integer rejectedBallots = 0;

    @Column(name = "spoiled_ballots", nullable = false)
    private Integer spoiledBallots = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private VoteStatus status = VoteStatus.PENDING;

    @Column(columnDefinition = "text")
    private String comments;

    @Column(name = "gps_location", columnDefinition = "geography(Point,4326)")
    private Point gpsLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private SystemUser verifiedBy;

    @Column(name = "date_verified")
    private LocalDateTime dateVerified;

    @Column(name = "client_ip", length = 100)
    private String clientIp;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "submission_hash", unique = true)
    private String submissionHash;

    @Column(name = "date_deleted")
    private LocalDateTime dateDeleted;
}
