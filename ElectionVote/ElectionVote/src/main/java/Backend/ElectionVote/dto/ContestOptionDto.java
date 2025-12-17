package Backend.ElectionVote.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for ContestOption used by admin API.
 */
@Data
public class ContestOptionDto {
    private UUID optionId;
    private UUID contestId;
    private UUID candidateId;
    private String optionLabel;
    private Integer optionOrder;
    private Boolean isActive;
    private LocalDateTime dateCreated;
}