package Backend.ElectionVote.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSearchRequest {

    private String q;          // optional: search by userName/first/last/email (basic)
    private String roleName;   // optional
    private Boolean enabled;
}
