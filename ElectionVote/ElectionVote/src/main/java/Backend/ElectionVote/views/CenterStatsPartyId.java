package Backend.ElectionVote.views;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite key for the view v_center_stats_party:
 *   (org_id, election_id, center_id)
 *
 * This is an @Embeddable used as the @EmbeddedId on the entity mapping the view.
 */
@Embeddable
public class CenterStatsPartyId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    public CenterStatsPartyId() {}

    public CenterStatsPartyId(UUID orgId, UUID electionId, UUID centerId) {
        this.orgId = orgId;
        this.electionId = electionId;
        this.centerId = centerId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getElectionId() {
        return electionId;
    }

    public void setElectionId(UUID electionId) {
        this.electionId = electionId;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public void setCenterId(UUID centerId) {
        this.centerId = centerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CenterStatsPartyId that = (CenterStatsPartyId) o;
        return Objects.equals(orgId, that.orgId) &&
                Objects.equals(electionId, that.electionId) &&
                Objects.equals(centerId, that.centerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, electionId, centerId);
    }
}