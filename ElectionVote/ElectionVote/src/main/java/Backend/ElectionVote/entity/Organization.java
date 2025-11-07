package Backend.ElectionVote.entity;

import Backend.ElectionVote.enums.OrganizationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.processing.Pattern;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "organization", uniqueConstraints = {
        @UniqueConstraint(name = "uq_org_subdomain", columnNames = "subdomain")
})
public class Organization {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "org_id", nullable = false, updatable = false)
    private UUID orgId;

    @Size(max = 120)
    @Column(name = "org_name", nullable = false, length = 100)
    private String orgName;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, length = 30)
    private OrganizationType organizationType;

    /** Optional link to a Party (only for party tenants) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", foreignKey = @ForeignKey(name = "organization_party_id_fkey"))
    private Party party;

    /** Branding & whitelabel */
    @Column(name = "logo_url")
    private String logoUrl;

    @Size(max = 20) // e.g., "#0A84FF" or rgba/short hex if you choose
    @Column(name = "primary_color", length = 9)
    private String primaryColor;

    @Column(name = "subdomain", unique = true, length = 63)
    private String subdomain;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @PrePersist
    void prePersist() {
        if (dateCreated == null) dateCreated = LocalDateTime.now();
    }


    // Getter & Setter
    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public @Size(max = 120) String getOrgName() {
        return orgName;
    }

    public void setOrgName(@Size(max = 120) String orgName) {
        this.orgName = orgName;
    }

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(OrganizationType organizationType) {
        this.organizationType = organizationType;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public @Size(max = 20) String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(@Size(max = 20) String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
