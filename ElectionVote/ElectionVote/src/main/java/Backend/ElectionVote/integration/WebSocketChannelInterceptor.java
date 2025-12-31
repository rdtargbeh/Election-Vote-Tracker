package Backend.ElectionVote.integration;

import Backend.ElectionVote.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final ChatRoomMemberRepository members;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var acc = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
            if (!(acc.getUser() instanceof WsPrincipal p)) {
                throw new IllegalStateException("No WS principal");
            }
            String dest = acc.getDestination();
            if (dest == null || !dest.startsWith("/topic/rooms/")) {
                throw new IllegalArgumentException("Invalid destination");
            }
            String[] parts = dest.split("/");
            if (parts.length < 4) throw new IllegalArgumentException("Room id missing");
            UUID roomId = UUID.fromString(parts[3]);

            boolean ok = members.existsByRoom_RoomIdAndUser_UserId(roomId, p.userId());
            if (!ok) throw new SecurityException("Not a member of this room");
        }
        return message;
    }



//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
//        StompCommand cmd = acc.getCommand();
//        if (cmd == null) return message;
//
//        // Only guard SUBSCRIBE
//        if (StompCommand.SUBSCRIBE.equals(cmd)) {
//            Object p = acc.getSessionAttributes() != null ? acc.getSessionAttributes().get("principal") : null;
//            if (!(p instanceof WsPrincipal principal)) throw new IllegalStateException("No WS principal");
//
//            String dest = acc.getDestination();
//            // Allowed topics: /topic/rooms/{roomId} and sub-paths like /topic/rooms/{roomId}/typing
//            if (dest == null || !dest.startsWith("/topic/rooms/")) {
//                throw new IllegalArgumentException("Invalid destination");
//            }
//
//            String[] parts = dest.split("/");
//            // /topic/rooms/{roomId}[...]
//            if (parts.length < 4) throw new IllegalArgumentException("Room id missing");
//            UUID roomId = UUID.fromString(parts[3]);
//
//            // Require membership
//            boolean ok = members.existsByRoom_RoomIdAndUser_UserId(roomId, principal.userId());
//            if (!ok) throw new SecurityException("Not a member of this room");
//        }
//
//        // Optionally gate SEND to /app/typing.{roomId}, etc.
//        if (StompCommand.SEND.equals(cmd)) {
//            String dest = acc.getDestination();
//            if (dest != null && dest.startsWith("/app/typing.")) {
//                String roomIdStr = dest.substring("/app/typing.".length());
//                UUID.fromString(roomIdStr); // validate format
//            }
//        }
//
//        return message;
//    }
}

