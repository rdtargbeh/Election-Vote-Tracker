package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.entity.Notification;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.repository.ChatRoomMemberRepository;
import Backend.ElectionVote.repository.NotificationRepository;
import Backend.ElectionVote.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImplementation implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    @Transactional
    public Notification send(Organization org,
                             SystemUser recipient,
                             String type,
                             String title,
                             String message,
                             String relatedTable,
                             UUID relatedId) {

        Notification n = Notification.builder()
                .organization(org)
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedTable(relatedTable)
                .relatedId(relatedId)
                .deliveryMethod("IN_APP")
                .priority("NORMAL")
                .build();

        return notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void notifyRoomMembersOnNewMessage(UUID roomId,
                                              UUID senderUserId,
                                              Organization org,
                                              String roomDisplayName,
                                              UUID messageId) {
        // Fetch enabled, non-muted members of the room (excluding sender)
        List<SystemUser> recipients =
                chatRoomMemberRepository.findActiveUsersToNotify(roomId, senderUserId);

        String title = "New Message";
        String msg = "New message in " + (roomDisplayName != null ? roomDisplayName : "this room");

        for (SystemUser u : recipients) {
            send(org, u, "CHAT", title, msg, "chat_message", messageId);
        }
    }
}