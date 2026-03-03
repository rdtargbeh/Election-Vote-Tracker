package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.ContestCategory;
import Backend.ElectionVote.enums.ContestScopeType;
import Backend.ElectionVote.enums.ContestStatus;
import Backend.ElectionVote.enums.ContestVoteMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContestCreateRequest {

    @NotNull
    private UUID electionId;

    @NotBlank
    private String contestName;

    private ContestCategory category = ContestCategory.OTHER;

    private ContestScopeType scopeType = ContestScopeType.NATIONAL;
    private UUID countyId;
    private UUID districtId;

    private ContestVoteMethod voteMethod = ContestVoteMethod.SINGLE_CHOICE;

    @Min(1)
    private Integer seats = 1;

    @Min(1)
    private Integer maxSelections = 1;

    private String description;

    private ContestStatus status = ContestStatus.DRAFT;

    private Boolean isActive = true;
}
