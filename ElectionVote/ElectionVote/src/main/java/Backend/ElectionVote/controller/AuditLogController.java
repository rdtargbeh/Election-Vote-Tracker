package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.AuditLogDto;
import Backend.ElectionVote.enums.ActivityType;
import Backend.ElectionVote.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    // Explicitly choose the JDBC implementation to resolve the "2 beans found" ambiguity
    @Qualifier("auditLogServiceJdbc")
    private final AuditLogService auditLogService;

    /**
     * Search audit logs for an organization with optional filters.
     *
     * @param orgId   organization/tenant id (path)
     * @param userId  optional user id filter
     * @param type    optional activity type filter
     * @param from    optional start datetime (ISO 8601)
     * @param to      optional end datetime (ISO 8601)
     * @param q       optional text search (entity_affected / action_description)
     * @param pageable Spring Data pagination + sorting
     */
    @GetMapping
    public Page<AuditLogDto> search(@PathVariable("orgId") UUID orgId,
                                    @RequestParam(value = "userId", required = false) UUID userId,
                                    @RequestParam(value = "type", required = false) ActivityType type,
                                    @RequestParam(value = "from", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                    @RequestParam(value = "to", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                    @RequestParam(value = "q", required = false) String q,
                                    Pageable pageable) {

        return auditLogService.search(orgId, userId, type, from, to, q, pageable);
    }

//    @GetMapping
//    public Page<AuditLogDto> search(@RequestParam(required = false) UUID orgId,
//                                    @RequestParam(required = false) UUID userId,
//                                    @RequestParam(required = false) ActivityType type,
//                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
//                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
//                                    @RequestParam(required = false) String q,
//                                    @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
//        return service.search(orgId, userId, type, from, to, q, pageable);
//    }

    // Optional: manual logging endpoint (handy for admin tools)
    @PostMapping
    public AuditLogDto create(@RequestParam UUID orgId,
                              @RequestParam UUID userId,
                              @RequestParam ActivityType type,
                              @RequestParam String entity,
                              @RequestParam String description) {
        return service.log(orgId, userId, type, entity, description);
    }
}