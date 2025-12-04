package Backend.ElectionVote.views;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only JPA mapping for the DB view: v_center_stats_party
 *
 * Notes:
 * - The view is read-only; annotate with @Immutable.
 * - Use an @EmbeddedId for the composite (org_id, election_id, center_id).
 * - Field names match the view columns; types chosen to map SQL -> Java reasonably.
 */
@Entity
@Table(name = "v_center_stats_party")
@Immutable
public class CenterStatsParty {

    @EmbeddedId
    private CenterStatsPartyId id;

    @Column(name = "center_code")
    private String centerCode;

    @Column(name = "center_name")
    private String centerName;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "county_id")
    private UUID countyId;

    @Column(name = "county_name")
    private String countyName;

    @Column(name = "registered_voters")
    private Integer registeredVoters;

    @Column(name = "ballots_cast")
    private Integer ballotsCast;

    @Column(name = "valid_votes")
    private Integer validVotes;

    @Column(name = "invalid_total")
    private Integer invalidTotal;

    @Column(name = "turnout_pct")
    private BigDecimal turnoutPct;

    @Column(name = "invalid_pct")
    private BigDecimal invalidPct;

    protected CenterStatsParty() {}

    public CenterStatsParty(CenterStatsPartyId id) {
        this.id = id;
    }

    // EmbeddedId accessor
    public CenterStatsPartyId getId() {
        return id;
    }

    public void setId(CenterStatsPartyId id) {
        this.id = id;
    }

    // Convenience getters mapping to embedded id parts
    public UUID getOrgId() {
        return id != null ? id.getOrgId() : null;
    }

    public UUID getElectionId() {
        return id != null ? id.getElectionId() : null;
    }

    public UUID getCenterId() {
        return id != null ? id.getCenterId() : null;
    }

    // Other getters
    public String getCenterCode() {
        return centerCode;
    }

    public String getCenterName() {
        return centerName;
    }

    public UUID getDistrictId() {
        return districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public UUID getCountyId() {
        return countyId;
    }

    public String getCountyName() {
        return countyName;
    }

    public Integer getRegisteredVoters() {
        return registeredVoters;
    }

    public Integer getBallotsCast() {
        return ballotsCast;
    }

    public Integer getValidVotes() {
        return validVotes;
    }

    public Integer getInvalidTotal() {
        return invalidTotal;
    }

    public BigDecimal getTurnoutPct() {
        return turnoutPct;
    }

    public BigDecimal getInvalidPct() {
        return invalidPct;
    }

    @Override
    public String toString() {
        return "CenterStatsParty{" +
                "id=" + id +
                ", centerCode='" + centerCode + '\'' +
                ", centerName='" + centerName + '\'' +
                ", districtId=" + districtId +
                ", districtName='" + districtName + '\'' +
                ", countyId=" + countyId +
                ", countyName='" + countyName + '\'' +
                ", registeredVoters=" + registeredVoters +
                ", ballotsCast=" + ballotsCast +
                ", validVotes=" + validVotes +
                ", invalidTotal=" + invalidTotal +
                ", turnoutPct=" + turnoutPct +
                ", invalidPct=" + invalidPct +
                '}';
    }
}