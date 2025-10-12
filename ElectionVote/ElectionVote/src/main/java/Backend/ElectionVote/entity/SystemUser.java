package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(
        name = "system_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_email", columnNames = "email")
        }
)
public class SystemUser {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(name = "user_name", nullable = false, unique = true, length = 30)
    private String userName;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    /** Relationships **/

    // Role reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
    private UserRole role;

    // Party reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", foreignKey = @ForeignKey(name = "fk_user_party"))
    private Party party;

    // County assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_county", foreignKey = @ForeignKey(name = "fk_user_county"))
    private County assignedCounty;

    /** Status flags **/
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    /** Audit fields **/
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    // Default organization (added after org table creation)
    @Column(name = "default_org_id")
    private UUID defaultOrgId;

    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastPasswordChange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_org_id")
    private Organization defaultOrg;

}
