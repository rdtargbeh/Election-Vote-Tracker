package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.ElectionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ElectionDto {
    private UUID electionId;
    private String electionName;
    private int year;
    private ElectionType electionType;
    private boolean isActive;
    private LocalDateTime dateCreated;
    private LocalDateTime dateUpdated;

}