package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;


@Entity
@Table(
        name = "election_candidate",
        uniqueConstraints = @UniqueConstraint(columnNames = {"election_id","candidate_id","polling_center_id"})
)

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ElectionCandidate {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "elect_id", nullable = false, updatable = false)
    private UUID electId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ec_election"))
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ec_candidate"))
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "polling_center_id",
            foreignKey = @ForeignKey(name = "fk_ec_center"))
    private PollingCenter pollingCenter; // nullable = nationwide or district-scoped via null
}
