package Backend.ElectionVote.uility;

import Backend.ElectionVote.enums.OrganizationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSearchRequest {

    private String q;
    private Boolean active;
    private OrganizationType type;
}
