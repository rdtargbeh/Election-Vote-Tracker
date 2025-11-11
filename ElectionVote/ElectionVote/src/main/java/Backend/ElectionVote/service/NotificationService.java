package Backend.ElectionVote.service;

import Backend.ElectionVote.entity.Notification;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;

import java.util.UUID;

public interface NotificationService {

    Notification send(Organization org,
                      SystemUser recipient,
                      String type,
                      String title,
                      String message,
                      String relatedTable,
                      UUID relatedId);

    /** Helper for chat: send “New Message” to room members except the sender. */
    void notifyRoomMembersOnNewMessage(UUID roomId,
                                       UUID senderUserId,
                                       Organization org,
                                       String roomDisplayName,
                                       UUID messageId);


}
