package Backend.ElectionVote.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class VoteSubmissionUpdateRequest {
    private Map<UUID,Integer> candidateVotes;
    private Integer ballotsCast, invalidBallots, blankBallots, rejectedBallots, spoiledBallots;
    private String comments;
    private Double latitude, longitude;
}
