package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.ContestCategory;
import Backend.ElectionVote.enums.ContestScopeType;
import Backend.ElectionVote.enums.ContestStatus;
import Backend.ElectionVote.enums.ContestVoteMethod;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Contest entity used by API and services.
 */
@Data
public class ContestDto {
    private UUID contestId;

    private UUID electionId;
    private String contestName;

    private ContestCategory category;
    private ContestScopeType scopeType;
    private UUID countyId;
    private UUID districtId;

    private ContestVoteMethod voteMethod;
    private Integer seats;
    private Integer maxSelections;

    private String description;

    private ContestStatus status;
    private Boolean isActive;

    private LocalDateTime dateCreated;
    private LocalDateTime dateUpdated;
}