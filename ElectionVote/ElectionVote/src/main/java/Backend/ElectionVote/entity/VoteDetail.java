package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(
        name = "vote_detail",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_submission_candidate",
                        columnNames = {"submission_id", "candidate_id"}
                )
        }
)
public class VoteDetail {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "detail_id", nullable = false, updatable = false)
    private UUID detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "vote_detail_org_id_fkey"))
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "vote_detail_submission_id_fkey"))
    private VoteSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id",
            foreignKey = @ForeignKey(name = "vote_detail_party_id_fkey"))
    private Party party; // nullable (ON DELETE SET NULL)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id",
            foreignKey = @ForeignKey(name = "vote_detail_candidate_id_fkey"))
    private Candidate candidate; // nullable (ON DELETE SET NULL)

    @NotNull
    @Min(0)
    @Column(name = "vote_count", nullable = false)
    private Integer voteCount;
}
