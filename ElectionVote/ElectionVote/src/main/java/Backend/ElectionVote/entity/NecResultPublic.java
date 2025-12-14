//package Backend.ElectionVote.entity;
//
//import Backend.ElectionVote.utility.NecResultPublicId;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.annotations.Immutable;
//import org.hibernate.type.SqlTypes;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.UUID;
//
///**
// * Public snapshot of validated center results for an election.
// *
// * This entity maps to nec_result_public which has a composite primary key (election_id, assigned_center_id).
// * Marked @Immutable because the public snapshot is managed by the publish process and should be read-only via JPA.
// */
//@Getter
//@Setter
//@NoArgsConstructor
//@Entity
//@Table(name = "nec_result_public",
//        indexes = {
//                @Index(name = "idx_nrp_election", columnList = "election_id"),
//                @Index(name = "idx_nrp_center", columnList = "assigned_center_id")
//        })
//@IdClass(NecResultPublicId.class)
//@Immutable
//public class NecResultPublic {
//
//    @Id
//    @Column(name = "election_id", nullable = false)
//    private UUID electionId;
//
//    @Id
//    @Column(name = "assigned_center_id", nullable = false)
//    private UUID assignedCenterId;
//
//    @Column(name = "center_code")
//    private String centerCode;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "candidate_votes", columnDefinition = "jsonb")
//    private Map<String, Integer> candidateVotes;
//
//    @Column(name = "total_registered_voters")
//    private Integer totalRegisteredVoters;
//
//    @Column(name = "ballots_cast")
//    private Integer ballotsCast;
//
//    @Column(name = "invalid_ballots")
//    private Integer invalidBallots;
//
//    @Column(name = "blank_ballots")
//    private Integer blankBallots;
//
//    @Column(name = "rejected_ballots")
//    private Integer rejectedBallots;
//
//    @Column(name = "spoiled_ballots")
//    private Integer spoiledBallots;
//
//    @Column(name = "source")
//    private String source;
//
//    @Column(name = "published_at")
//    private LocalDateTime publishedAt;
//}