package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.ChatMessageCreateRequest;
import Backend.ElectionVote.dto.ChatMessageDto;
import Backend.ElectionVote.entity.ChatMessage;
import Backend.ElectionVote.entity.ChatRoom;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.mapper.ChatMessageMapper;
import Backend.ElectionVote.repository.ChatMessageRepository;
import Backend.ElectionVote.repository.ChatRoomRepository;
import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.service.ChatMessageService;
import Backend.ElectionVote.service.NotificationService;
import Backend.ElectionVote.utility.ChatMessageCreatedEvent;
import Backend.ElectionVote.utility.ChatMessageEditRequest;
import Backend.ElectionVote.utility.TenantUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageServiceImplementation implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;
    private final ChatMessageRepository messages;
    private final ChatRoomRepository rooms;
    private final OrganizationRepository organizations;

    private final ChatMessageMapper mapper = new ChatMessageMapper();
    private final ApplicationEventPublisher events;



    @Override
    public ChatMessageDto sendInTenant(ChatMessageCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("Request is required");

        Organization org = requireOrg();
        SystemUser sender = requirePrincipal();

        ChatRoom room = rooms.findInOrg(org.getOrgId(), req.getRoomId())
                .orElseThrow(() -> new NoSuchElementException("Room not found"));

        // Idempotency: if message with clientGuid exists, return existing DTO (do NOT notify again)
        if (req.getClientGuid() != null) {
            var existing = messages.findByClientGuid(req.getClientGuid());
            if (existing.isPresent()) {
                return mapper.toDTO(existing.get());
            }
        }

        ChatMessage parent = null;
        if (req.getReplyTo() != null) {
            parent = messages.findById(req.getReplyTo())
                    .orElseThrow(() -> new NoSuchElementException("replyTo message not found"));
            if (!parent.getRoom().getRoomId().equals(room.getRoomId())) {
                throw new IllegalArgumentException("replyTo must be in the same room");
            }
        }

        ChatMessage entity = mapper.toEntity(req, org, room, sender, parent);
        validateContent(entity.getContent(), entity.getContentType());
        ChatMessage saved = messages.save(entity);

        // Fire after-commit event to notify room members
        String roomDisplayName = (room.getName() == null || room.getName().isBlank())
                ? "Direct Message" : room.getName();
        events.publishEvent(new ChatMessageCreatedEvent(
                room.getRoomId(), saved.getMessageId(), sender.getUserId(), org, roomDisplayName
        ));

        return mapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ChatMessageDto> listInTenant(UUID roomId, Pageable pageable) {
        Organization org = requireOrg();
        ChatRoom room = rooms.findInOrg(org.getOrgId(), roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found"));
        return messages.findRecent(room.getRoomId(), pageable).map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatMessageDto> syncSinceInTenant(UUID roomId, LocalDateTime since) {
        Organization org = requireOrg();
        ChatRoom room = rooms.findInOrg(org.getOrgId(), roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found"));
        return messages.findSince(room.getRoomId(), since).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ChatMessageDto editInTenant(UUID messageId, ChatMessageEditRequest req) {
        if (req == null) throw new IllegalArgumentException("Request is required");
        Organization org = requireOrg();
        SystemUser me = requirePrincipal();

        ChatMessage m = messages.fetchWithSender(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        if (!m.getOrganization().getOrgId().equals(org.getOrgId())) {
            throw new SecurityException("Cross-tenant access forbidden");
        }
        if (!m.getSender().getUserId().equals(me.getUserId())) {
            throw new SecurityException("Only the sender can edit this message");
        }

        validateContent(req.getContent(), m.getContentType());
        m.setContent(req.getContent());
        m.setDateEdited(LocalDateTime.now());

        return mapper.toDTO(m);
    }

    @Override
    public void deleteInTenant(UUID messageId) {
        Organization org = requireOrg();
        SystemUser me = requirePrincipal();

        ChatMessage m = messages.fetchWithSender(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
        if (!m.getOrganization().getOrgId().equals(org.getOrgId())) {
            throw new SecurityException("Cross-tenant access forbidden");
        }
        if (!m.getSender().getUserId().equals(me.getUserId())) {
            throw new SecurityException("Only the sender can delete this message");
        }
        m.setDateDeleted(LocalDateTime.now());
    }

    /* -------- helpers -------- */

    private void validateContent(String content, String type) {
        if (content != null && content.length() > 4000) {
            throw new IllegalArgumentException("Message too long (max 4000 chars)");
        }
        // optionally validate contentType: TEXT/IMAGE/FILE/SYSTEM
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("contentType is required");
        }
    }


    private Organization requireOrg() {
        UUID orgId = TenantUtils.requireTenantOrg();
        return organizations.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
    }

    private SystemUser requirePrincipal() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof SystemUser me)) {
            throw new IllegalStateException("Authenticated user required");
        }
        return (SystemUser) auth.getPrincipal();
    }



    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        // Persist message
        ChatMessage saved = chatMessageRepository.save(message);

        // Notify room members (except sender)
        ChatRoom room = saved.getRoom();
        notificationService.notifyRoomMembersOnNewMessage(
                room.getRoomId(),
                saved.getSender().getUserId(),
                room.getOrganization(),
                room.getName(),                  // null for DM is fine
                saved.getMessageId()
        );

        return saved;
    }

    public ChatMessage get(UUID id) {
        return chatMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
    }
}
