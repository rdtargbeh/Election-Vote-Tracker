package Backend.ElectionVote.utility;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchRequest {

    private String q;
    private Boolean active;
    private UUID roleId;
    private UUID partyId;
    private UUID countyId;

//    public UserSearchRequest(String q, Boolean active, UUID roleId, UUID partyId, UUID countyId) {
//        this.q = q;
//        this.active = active;
//        this.roleId = roleId;
//        this.partyId = partyId;
//        this.countyId = countyId;
//    }
}
