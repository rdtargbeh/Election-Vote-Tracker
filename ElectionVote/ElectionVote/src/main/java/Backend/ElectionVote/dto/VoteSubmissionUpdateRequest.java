package Backend.ElectionVote.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class VoteSubmissionUpdateRequest {
    private Map<String,Integer> candidateVotes;
    private Integer ballotsCast;
    private Integer invalidBallots;
    private Integer blankBallots;
    private Integer rejectedBallots;
    private Integer spoiledBallots;
    private String comments;
    private Double latitude, longitude;
}
