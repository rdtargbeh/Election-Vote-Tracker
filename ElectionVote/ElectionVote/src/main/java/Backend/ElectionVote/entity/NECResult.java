package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "nec_result")
public class NECResult {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "result_id", updatable = false, nullable = false)
    private UUID resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private PollingCenter pollingCenter;

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

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (uploadTime == null) {
            uploadTime = LocalDateTime.now();
        }
    }

}
