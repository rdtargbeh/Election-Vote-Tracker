package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Table(
        name = "vote_tally",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_org_election_candidate",
                        columnNames = {"org_id", "election_id", "candidate_id"}
                )
        }
)
public class VoteTally {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "tally_id", nullable = false, updatable = false)
    private UUID tallyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false,
            foreignKey = @ForeignKey(name = "vote_tally_election_id_fkey"))
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false,
            foreignKey = @ForeignKey(name = "vote_tally_org_id_fkey"))
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id",
            foreignKey = @ForeignKey(name = "vote_tally_party_id_fkey"))
    private Party party; // nullable (ON DELETE SET NULL)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id",
            foreignKey = @ForeignKey(name = "vote_tally_candidate_id_fkey"))
    private Candidate candidate; // nullable (ON DELETE SET NULL)

    @NotNull
    @Min(0)
    @Column(name = "vote_count", nullable = false)
    private Integer voteCount;


    @Column(name = "last_recomputed_at", nullable = false)
    private LocalDateTime lastRecomputedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recomputed_by",
            foreignKey = @ForeignKey(name = "vote_tally_recomputed_by_fkey"))
    private SystemUser recomputedBy; // nullable if system job



}
