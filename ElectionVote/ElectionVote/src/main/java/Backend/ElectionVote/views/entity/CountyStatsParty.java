package Backend.ElectionVote.views.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only JPA mapping of the database view v_county_stats_party.
 */
@Entity
@Table(name = "v_county_stats_party")
@Immutable
@Getter
@NoArgsConstructor
public class CountyStatsParty {

    @EmbeddedId
    private CountyStatsPartyId id;

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

    @Column(name = "turnout_pct", precision = 10, scale = 6)
    private BigDecimal turnoutPct;

    @Column(name = "invalid_pct", precision = 10, scale = 6)
    private BigDecimal invalidPct;
}
