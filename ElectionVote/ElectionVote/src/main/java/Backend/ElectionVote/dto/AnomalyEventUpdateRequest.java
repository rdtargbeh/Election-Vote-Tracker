package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.AnomalyKind;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class AnomalyEventUpdateRequest {
    private UUID centerId; // optional change
    private AnomalyKind kind;
    private Map<String, Object> details;
}
