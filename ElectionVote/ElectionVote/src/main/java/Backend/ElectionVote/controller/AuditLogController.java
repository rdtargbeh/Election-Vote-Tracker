package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.AuditLogDto;
import Backend.ElectionVote.enums.ActivityType;
import Backend.ElectionVote.service.AuditLogService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public Page<AuditLogDto> search(@RequestParam(required = false) UUID orgId,
                                    @RequestParam(required = false) UUID userId,
                                    @RequestParam(required = false) ActivityType type,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                    @RequestParam(required = false) String q,
                                    @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(orgId, userId, type, from, to, q, pageable);
    }

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