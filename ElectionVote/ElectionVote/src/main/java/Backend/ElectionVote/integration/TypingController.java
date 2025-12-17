package Backend.ElectionVote.integration;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TypingController {

    private final SimpMessagingTemplate ws;

    // Client SENDs to: /app/typing.{roomId}
    // We broadcast to:  /topic/rooms/{roomId}/typing
    @MessageMapping("/typing.{roomId}")
    public void typing(@DestinationVariable String roomId, TypingSignal payload) {
        ws.convertAndSend("/topic/rooms/" + roomId + "/typing", Map.of(
                "type", "typing",
                "typing", payload != null && payload.isTyping()
        ));
    }
}