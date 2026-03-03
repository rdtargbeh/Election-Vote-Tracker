package Backend.ElectionVote.dto;


import Backend.ElectionVote.enums.ContestCategory;
import Backend.ElectionVote.enums.ContestScopeType;
import Backend.ElectionVote.enums.ContestStatus;
import Backend.ElectionVote.enums.ContestVoteMethod;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContestUpdateRequest {

    // allow update but service will gate it (e.g., forbid electionId change when not DRAFT)
    private UUID electionId;
    private String contestName;
    private ContestCategory category;
    private ContestScopeType scopeType;
    private UUID countyId;
    private UUID districtId;
    private ContestVoteMethod voteMethod;

    @Min(1)
    private Integer seats;

    @Min(1)
    private Integer maxSelections;
    private String description;
    private ContestStatus status;
    private Boolean isActive;
}
