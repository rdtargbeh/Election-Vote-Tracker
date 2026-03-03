package Backend.ElectionVote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VoteSubmissionFlagRequest {
    @NotNull
    private UUID actorUserId;   // observer / NEC user
    @NotNull
    private Boolean flagged;
    private String comments;  // required
}
