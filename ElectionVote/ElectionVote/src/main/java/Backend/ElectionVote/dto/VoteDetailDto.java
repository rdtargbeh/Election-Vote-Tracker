package Backend.ElectionVote.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VoteDetailDto {
    private UUID detailId;

    private UUID orgId;
    private UUID submissionId;

    private UUID partyId;
    private String partyName;
    private String partyAbbreviation;

    private UUID candidateId;
    private String candidateName;

    private Integer voteCount;
}
