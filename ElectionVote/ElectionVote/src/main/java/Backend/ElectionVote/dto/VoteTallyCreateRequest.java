package Backend.ElectionVote.dto;


import Backend.ElectionVote.entity.SystemUser;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class VoteTallyCreateRequest {
    @NotNull private UUID orgId;
    private UUID electionId;

    // either candidateId or partyId is expected (candidateId preferred)
    private UUID partyId;
    private UUID candidateId;

    @NotNull
    @Min(0)
    private Integer voteCount;

    //    @NotNull private UUID submissionId;
}
