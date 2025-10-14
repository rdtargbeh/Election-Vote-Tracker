package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_role",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_role_name", columnNames = "role_name")
)
public class UserRole {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 30)
    private RoleName roleName;

    @Column(name = "description", length = 150)
    private String description;

    // Optional builder for controlled construction (no relations here, so it's safe)
    @Builder(toBuilder = true)
    public UserRole(RoleName roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

}
