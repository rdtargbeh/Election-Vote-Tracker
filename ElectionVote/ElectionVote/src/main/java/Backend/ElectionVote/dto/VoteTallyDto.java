package Backend.ElectionVote.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VoteTallyDto {
    private UUID tallyId;

    private UUID orgId;
    private String orgName;

    private UUID electionId;
    private String electionName;
//    private UUID submissionId;

    private UUID partyId;
    private String partyName;
    private String abbreviation;

    private UUID candidateId;
    private String fullName;

    private Integer voteCount;

    private LocalDateTime lastRecomputedAt;
    private UUID recomputedByUserId;
}
