package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.entity.ChatMessage;
import Backend.ElectionVote.entity.ChatRoom;
import Backend.ElectionVote.repository.ChatMessageRepository;
import Backend.ElectionVote.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImplementation {

    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;

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
