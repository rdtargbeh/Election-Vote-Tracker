package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.OrganizationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDto {

    private UUID orgId;
    private String orgName;
    private OrganizationType organizationType;
    private String logoUrl;
    private String primaryColor;
    private String subdomain;
    private boolean active;
    private LocalDateTime dateCreated;

    private UUID partyId;
    private String partyName;
    private String partyAbbreviation;
}
