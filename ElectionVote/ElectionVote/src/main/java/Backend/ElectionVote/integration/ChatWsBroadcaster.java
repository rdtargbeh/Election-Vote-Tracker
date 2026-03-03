package Backend.ElectionVote.integration;

import Backend.ElectionVote.utility.ChatMessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;


@Component
@RequiredArgsConstructor
public class ChatWsBroadcaster {

    private final SimpMessagingTemplate ws;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ChatMessageCreatedEvent evt) {
        // Minimal payload; client can fetch full DTO if needed
        ws.convertAndSend("/topic/rooms/" + evt.roomId(), Map.of(
                "type", "chat_message_created",
                "roomId", evt.roomId().toString(),
                "messageId", evt.messageId().toString(),
                "senderId", evt.senderUserId().toString()
        ));
    }

    // Optional: also handle edited/deleted message events similarly
}
