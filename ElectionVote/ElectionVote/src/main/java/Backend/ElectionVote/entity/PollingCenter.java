package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;


@Entity
@Table(
        name = "polling_center",
        uniqueConstraints = @UniqueConstraint(name = "uq_center_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_center_district", columnList = "district_id"),
                @Index(name = "idx_center_name_ci", columnList = "center_name") // optional, helps LIKE
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PollingCenter {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(name = "center_name", nullable = false, length = 150)
    private String centerName;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "registered_voters", nullable = false)
    private int registeredVoters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_center_district"))
    private District district;

}
