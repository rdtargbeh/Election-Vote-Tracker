package Backend.ElectionVote.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class VoteSubmissionCreateRequest {
    @NotNull private UUID orgId;
    @NotNull private UUID electionId;
    @NotNull private UUID centerId;
    @NotNull private UUID agentId;
    @NotNull private UUID placeId;

    @NotNull @NotEmpty
    private Map<@NotNull UUID, @Min(0) Integer> candidateVotes; // candidateId -> votes

    @NotNull
    @Min(0) private Integer ballotsCast;
    @Min(0) private Integer invalidBallots = 0;
    @Min(0) private Integer blankBallots = 0;
    @Min(0) private Integer rejectedBallots = 0;
    @Min(0) private Integer spoiledBallots = 0;
    @Min(0) private Integer discrepency = 0;

    private String comments;

    // optional device metadata + GPS
    private String clientIp; private String userAgent;
    @DecimalMin("-90.0") @DecimalMax("90.0")  private Double latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
}
