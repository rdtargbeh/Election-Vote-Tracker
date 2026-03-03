package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ChatRoomDmDto;

import java.util.List;
import java.util.UUID;

public interface ChatRoomDmService {
    ChatRoomDmDto openOrGet(UUID otherUserId);
    List<ChatRoomDmDto> myDms();
}
