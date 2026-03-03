package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.AnomalyKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEventDto {
    private UUID anomalyId;
    private UUID orgId;
    private UUID electionId;
    private UUID centerId;   // nullable
    private AnomalyKind kind;
    private Map<String, Object> details;
    private LocalDateTime dateCreated;
}
