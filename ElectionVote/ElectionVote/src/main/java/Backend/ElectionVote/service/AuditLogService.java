package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.AuditLogDto;
import java.time.LocalDateTime;
import java.util.UUID;

import Backend.ElectionVote.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    // Core writer
    AuditLogDto log(UUID orgId, UUID userId, ActivityType type, String entity, String description);

    // Convenience helpers
    default AuditLogDto logCreate(UUID orgId, UUID userId, String entity, String description) {
        return log(orgId, userId, ActivityType.CREATE, entity, description);
    }
    default AuditLogDto logUpdate(UUID orgId, UUID userId, String entity, String description) {
        return log(orgId, userId, ActivityType.UPDATE, entity, description);
    }
    default AuditLogDto logDelete(UUID orgId, UUID userId, String entity, String description) {
        return log(orgId, userId, ActivityType.DELETE, entity, description);
    }
    default AuditLogDto logVerify(UUID orgId, UUID userId, String entity, String description) {
        return log(orgId, userId, ActivityType.VERIFY, entity, description);
    }
    default AuditLogDto logReject(UUID orgId, UUID userId, String entity, String description) {
        return log(orgId, userId, ActivityType.REJECT, entity, description);
    }
    default AuditLogDto logUpload(UUID orgId, UUID userId, String entity, String description) {
        return log(orgId, userId, ActivityType.UPLOAD, entity, description);
    }

    // Reader
    Page<AuditLogDto> search(UUID orgId, UUID userId, ActivityType type,
                             LocalDateTime from, LocalDateTime to, String q, Pageable pageable);
}
