package Backend.ElectionVote.utility;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
//@AllArgsConstructor
@Builder
public class PartySearchRequest {
    private String q; // searches name/abbreviation

    public PartySearchRequest(String q) {
        this.q = q;
    }
}
