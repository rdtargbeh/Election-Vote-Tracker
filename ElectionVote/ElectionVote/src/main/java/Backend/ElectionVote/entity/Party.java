package Backend.ElectionVote.entity;

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
        name = "party",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_party_name", columnNames = "party_name"),
                @UniqueConstraint(name = "uq_party_abbreviation", columnNames = "abbreviation")
        }
)
public class Party {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "party_id", nullable = false, updatable = false)
    private UUID partyId;

    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    @Column(name = "abbreviation", nullable = false, length = 10)
    private String abbreviation;

    @Column(name = "logo_url")
    private String logoUrl;


    // GETTER & SETTER
    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
