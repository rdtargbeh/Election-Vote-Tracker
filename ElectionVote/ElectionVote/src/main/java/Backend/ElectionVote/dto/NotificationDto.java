package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.DeliveryMethod;
import Backend.ElectionVote.enums.NotificationPriority;
import Backend.ElectionVote.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDto {
    private UUID notificationId;
    private UUID orgId;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private String relatedTable;
    private UUID relatedId;
    private boolean isRead;
    private boolean isSeen;
    private LocalDateTime dateCreated;
    private LocalDateTime dateRead;
    private LocalDateTime dateExpires;
    private NotificationPriority priority;
    private DeliveryMethod deliveryMethod;
    private UUID createdBy;
    private String idempotencyKey;
}
