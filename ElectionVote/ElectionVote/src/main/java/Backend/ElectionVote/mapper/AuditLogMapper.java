package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.AuditLogDto;
import Backend.ElectionVote.entity.AuditLog;

public class AuditLogMapper {

    public AuditLogDto toDTO(AuditLog a) {
        if (a == null) return null;

        String orgName = (a.getOrganization() != null) ? a.getOrganization().getOrgName() : null;

        String userName = null;
        if (a.getUser() != null) {
            String first = a.getUser().getFirstName();
            String last = a.getUser().getLastName();
            first = first == null ? "" : first.trim();
            last = last == null ? "" : last.trim();
            String combined = (first + " " + last).trim();
            userName = combined.isEmpty() ? null : combined;
        }

        return AuditLogDto.builder()
                .logId(a.getLogId())
                .orgId(a.getOrganization() != null ? a.getOrganization().getOrgId() : null)
                .orgName(orgName)
                .userId(a.getUser() != null ? a.getUser().getUserId() : null)
                .userName(userName)
                .activityType(a.getActivityType())
                .entityAffected(a.getEntityAffected())
                .actionDescription(a.getActionDescription())
                .metadata(a.getMetadata())
                .dateCreated(a.getDateCreated())
                .build();
    }


}