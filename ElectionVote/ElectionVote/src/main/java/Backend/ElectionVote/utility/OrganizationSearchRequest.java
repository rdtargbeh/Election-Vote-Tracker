package Backend.ElectionVote.utility;

import Backend.ElectionVote.enums.OrganizationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
//@AllArgsConstructor
@Builder
public class OrganizationSearchRequest {

    private String q;
    private Boolean active;
    private OrganizationType type;

    public OrganizationSearchRequest(String q, Boolean active, OrganizationType type) {
        this.q = q;
        this.active = active;
        this.type = type;
    }
}
