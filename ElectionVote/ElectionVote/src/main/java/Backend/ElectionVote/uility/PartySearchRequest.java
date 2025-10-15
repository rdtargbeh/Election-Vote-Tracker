package Backend.ElectionVote.uility;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartySearchRequest {
    private String q; // searches name/abbreviation
}
