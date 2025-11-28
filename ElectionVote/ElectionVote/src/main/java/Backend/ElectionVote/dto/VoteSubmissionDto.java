package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.VoteStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Data
@Builder
public class VoteSubmissionDto {
    private UUID submissionId;

    private UUID orgId;
    private String orgName;

    private UUID electionId;
    private String electionName;
    private int year;

    // Polling Center
    private UUID centerId;
    private String centerCode;
    private String centerName;

    // Polling Place
    private UUID placeId;
    private String placeCode;
    private Integer placeNumber;
    private String placeLabel;

    private UUID agentId;
    private String agentName;

    private LocalDateTime submissionTime;

    @JsonIgnore
    private String candidateVotesJson; // JSON string (candidateId -> votes)
    private Map<UUID, Integer> candidateVotes;

    private Integer ballotsCast;
    private Integer invalidBallots;
    private Integer blankBallots;
    private Integer rejectedBallots;
    private Integer spoiledBallots;
    private Integer discrepency ;

    private VoteStatus status;
    private String comments;

    private Double latitude;
    private Double longitude;

    private UUID verifiedBy;
    private String verifiedByName;
    private LocalDateTime dateVerified;

    private String clientIp;
    private String userAgent;
    private String submissionHash;

    private Integer version;

    // NEW: derived helpers for UI (can be set by service or mapper)
    private Integer validVotes;    // sum of candidateVotes
    private Integer invalidTotal;  // invalid + blank + rejected + spoiled

    private Double turnoutPct;     // ballotsCast / registeredVoters * 100
    private Double invalidPct;     // invalidTotal / ballotsCast * 100

}
