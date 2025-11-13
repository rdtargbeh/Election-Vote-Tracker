package Backend.ElectionVote.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import Backend.ElectionVote.enums.ActivityType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogDto {
    private UUID logId;
    private UUID orgId;
    private UUID userId;
    private ActivityType activityType;
    private String entityAffected;
    private String actionDescription;
    private LocalDateTime timestamp;
}