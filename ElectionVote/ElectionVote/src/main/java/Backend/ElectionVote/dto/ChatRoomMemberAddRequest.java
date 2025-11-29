package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.ChatMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ChatRoomMemberAddRequest {
    @NotNull
    private UUID userId;
    @NotNull private ChatMemberRole roleName; // MEMBER/ADMIN
}