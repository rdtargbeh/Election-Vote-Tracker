package Backend.ElectionVote.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CandidateDto {
    private UUID candidateId;
    private String fullName;
    private String position;
    private UUID partyId;
    private String partyName;
    private String partyAbbreviation;
    private String photoUrl;
    private boolean isActive;
}