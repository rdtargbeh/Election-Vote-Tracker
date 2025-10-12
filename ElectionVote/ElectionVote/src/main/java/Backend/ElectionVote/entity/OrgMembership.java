package Backend.ElectionVote.entity;

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
@Table(
        name = "org_membership",
        uniqueConstraints = @UniqueConstraint(name = "uq_org_user", columnNames = {"org_id", "user_id"})
)
public class OrgMembership {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "membership_id", nullable = false, updatable = false)
    private UUID membershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ord_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_org"))
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_user"))
    private SystemUser user;

    @Column(name = "role_name", nullable = false, length = 30)
    private String roleName;

    @Column(name = "is_enabled", nullable = false)
    private  boolean isEnabled = true;
}
