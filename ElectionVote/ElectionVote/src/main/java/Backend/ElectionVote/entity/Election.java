package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.ElectionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "election")
public class Election {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "election_id", nullable = false, updatable = false)
    private UUID electionId;

    @Column(name = "election_name", nullable = false, length = 100)
    private  String electionName;

    @Column(name = "year", nullable = false)
    private  int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ElectionType electionType;

    @Column(name = "is_active", nullable = false)
    private  boolean isActive;
}
