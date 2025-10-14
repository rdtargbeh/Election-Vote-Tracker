package Backend.ElectionVote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Objects;
import java.util.UUID;

/**
 * Tenant-scoped membership tying a {@link SystemUser} to an {@link Organization}.
 *
 * <p>
 * DB contract (from your DDL):
 * <ul>
 *   <li><b>UNIQUE (org_id, user_id)</b> – a user can have at most one membership per org.</li>
 *   <li><b>role_name</b> – textual role within this org (e.g., ADMIN, PARTY_ADMIN, AGENT...).</li>
 *   <li><b>is_enabled</b> – toggle access to this tenant without deleting history.</li>
 * </ul>
 *
 * <p>
 * Notes:
 * <ul>
 *   <li>We keep {@code roleName} as text to mirror the table and stay flexible per-tenant.</li>
 *   <li>Ownership of business rules (who can set which role) stays in the service/auth layer.</li>
 *   <li>This entity intentionally has no timestamps because the table doesn’t define them.</li>
 * </ul>
 */

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

    /** Surrogate key for the membership row. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "membership_id", nullable = false, updatable = false)
    private UUID membershipId;

    /**
     * Owning organization (tenant).
     *
     * <p><b>Important:</b> In your snippet the column was {@code ord_id} (typo).
     * The table uses {@code org_id}, so we fix it here to match the DDL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ord_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_org"))
    private Organization organization;

    /** User who is a member of the organization. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_user"))
    private SystemUser user;

    /**
     * Role name (text) within this org.
     * Keep aligned with your platform roles: ADMIN, PARTY_ADMIN, AGENT, OBSERVER, SUPERVISOR, COORDINATOR.
     */
    @Column(name = "role_name", nullable = false, length = 30)
    private String roleName;

    /** Enable/disable this membership without deleting it. */
    @Column(name = "is_enabled", nullable = false)
    private  boolean isEnabled = true;


    // ---------------------------------------------------------------------
    // Equality: use primary key when available; fallback to business key
    // -------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrgMembership that)) return false;

        // Prefer PK if both are persisted
        if (membershipId != null && that.membershipId != null) {
            return membershipId.equals(that.membershipId);
        }
        // Fallback: composite unique (org_id, user_id)
        return Objects.equals(organization, that.organization)
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        if (membershipId != null) return membershipId.hashCode();
        return Objects.hash(organization, user);
    }

    @Override
    public String toString() {
        // Avoid triggering lazy loads: print only IDs where possible
        UUID orgId = organization != null ? organization.getOrgId() : null;
        UUID userId = user != null ? user.getUserId() : null;
        return "OrgMembership{" +
                "membershipId=" + membershipId +
                ", orgId=" + orgId +
                ", userId=" + userId +
                ", roleName='" + roleName + '\'' +
                ", enabled=" + isEnabled +
                '}';
    }
}
