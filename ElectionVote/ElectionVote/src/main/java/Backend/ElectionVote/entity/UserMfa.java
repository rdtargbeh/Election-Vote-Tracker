package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.MfaMethod;
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
@Table(name = "user_mfa")
public class UserMfa {

    // PK is also the FK to system_users.user_id
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Link back to the user (shares same PK via @MapsId) */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "user_mfa_user_id_fkey"))
    private SystemUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 20)
    private MfaMethod method; // null = not set up yet

    @Column(name = "secret", columnDefinition = "text")
    private String secret; // store hashed/encrypted (do NOT return in APIs)

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @PrePersist @PreUpdate
    void stamp() { this.dateUpdated = LocalDateTime.now(); }
}
