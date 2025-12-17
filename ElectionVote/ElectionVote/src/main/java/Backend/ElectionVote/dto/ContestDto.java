package Backend.ElectionVote.dto;

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
    private String contestType;
    private String description;
    private Boolean isActive;
    private LocalDateTime dateCreated;
}