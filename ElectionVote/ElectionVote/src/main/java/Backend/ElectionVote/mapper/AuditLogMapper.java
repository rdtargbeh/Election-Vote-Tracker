package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.AuditLogDto;
import Backend.ElectionVote.entity.AuditLog;

public class AuditLogMapper {
    public AuditLogDto toDTO(AuditLog a) {
        return AuditLogDto.builder()
                .logId(a.getLogId())
                .orgId(a.getOrganization() != null ? a.getOrganization().getOrgId() : null)
                .userId(a.getUser() != null ? a.getUser().getUserId() : null)
                .activityType(a.getActivityType())
                .entityAffected(a.getEntityAffected())
                .actionDescription(a.getActionDescription())
                .timestamp(a.getTimestamp())
                .build();
    }
}