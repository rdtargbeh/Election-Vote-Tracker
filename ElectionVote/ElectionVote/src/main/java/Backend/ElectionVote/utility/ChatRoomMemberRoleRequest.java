package Backend.ElectionVote.utility;

import Backend.ElectionVote.enums.ChatMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomMemberRoleRequest {
    @NotNull
    private ChatMemberRole roleName;
}
