package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.NotificationCreateRequest;
import Backend.ElectionVote.dto.NotificationDto;
import Backend.ElectionVote.enums.DeliveryMethod;
import Backend.ElectionVote.enums.NotificationType;
import Backend.ElectionVote.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;


    @PostMapping
    public List<NotificationDto> publish(@Valid @RequestBody NotificationCreateRequest req) {
        return service.publish(req);
    }


    @GetMapping
    public Page<NotificationDto> search(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) DeliveryMethod method,
            @RequestParam(required = false) Boolean unread,
            Pageable pageable
    ) {
        return service.search(orgId, userId, type, method, unread, pageable);
    }

    @GetMapping("/unread-count/{userId}")
    public long unreadCount(@RequestParam UUID userId) {
        return service.unreadCount(userId);
    }

    @PostMapping("/mark-read/{userId}")
    public int markRead(@PathVariable UUID userId, @RequestBody List<UUID> ids) {
        return service.markRead(userId, ids);
    }

    @PostMapping("/mark-seen/{userId}")
    public int markSeen(@PathVariable UUID userId, @RequestBody List<UUID> ids) {
        return service.markSeen(userId, ids);
    }

}
