package Backend.ElectionVote.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgMembershipDto {

    private UUID membershipId;
    private UUID orgId;
    private UUID userId;
    private String roleName;   // e.g., ADMIN, PARTY_ADMIN, ...
    private boolean enabled;
}
