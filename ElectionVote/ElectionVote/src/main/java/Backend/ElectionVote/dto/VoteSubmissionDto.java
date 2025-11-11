package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.VoteStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
public class VoteSubmissionDto {
    private UUID submissionId;
    private UUID orgId;      private String orgName;
    private UUID electionId; private String electionName; private int year;
    private UUID centerId;   private String centerCode;   private String centerName;
    private UUID agentId;    private String agentName;
    private LocalDateTime submissionTime;
    private String candidateVotesJson; // JSON string (candidateId -> votes)
    private Integer ballotsCast, invalidBallots, blankBallots, rejectedBallots, spoiledBallots;
    private VoteStatus status;
    private String comments;
    private Double latitude, longitude;
    private UUID verifiedBy; private String verifiedByName;
    private LocalDateTime dateVerified;
    private String clientIp, userAgent, submissionHash;
    private Integer version;
}
