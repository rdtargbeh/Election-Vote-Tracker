package Backend.ElectionVote.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VoteDetailCreateRequest {
    @NotNull private UUID orgId;
    @NotNull private UUID submissionId;

    // either candidateId or partyId is expected (candidateId preferred)
    private UUID partyId;
    private UUID candidateId;

    @NotNull
    @Min(0)
    private Integer voteCount;
}
