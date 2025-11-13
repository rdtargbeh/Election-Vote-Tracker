package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ChatMessageCreateRequest;
import Backend.ElectionVote.dto.ChatMessageDto;
import Backend.ElectionVote.dto.ChatRoomMemberAddRequest;
import Backend.ElectionVote.dto.ChatRoomMemberDto;
import Backend.ElectionVote.utility.ChatRoomMemberMuteRequest;
import Backend.ElectionVote.utility.ChatRoomMemberRoleRequest;
import Backend.ElectionVote.utility.ChatRoomMemberSeenRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatRoomMemberService {
    Page<ChatRoomMemberDto> listInTenant(UUID roomId, Pageable pageable);
    ChatRoomMemberDto addInTenant(UUID roomId, ChatRoomMemberAddRequest req);
    void removeInTenant(UUID roomId, UUID userId);
    ChatRoomMemberDto setRoleInTenant(UUID roomId, UUID userId, ChatRoomMemberRoleRequest req);
    ChatRoomMemberDto setMutedInTenant(UUID roomId, UUID userId, ChatRoomMemberMuteRequest req);
    void markSeenInTenant(UUID roomId, UUID userId, ChatRoomMemberSeenRequest req);

    long unreadCountInTenant(UUID roomId, UUID userId);

    ChatMessageDto sendInTenant(ChatMessageCreateRequest req);
}
