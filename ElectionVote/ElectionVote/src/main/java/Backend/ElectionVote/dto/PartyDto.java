package Backend.ElectionVote.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyDto {

    private UUID partyId;
    private String partyName;
    private String abbreviation;
    private String logoUrl;

}
