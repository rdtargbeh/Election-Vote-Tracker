package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ChatReactionDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ChatReactionService {
    ChatReactionDto toggle(UUID messageId, String emoji);  // adds if missing, removes if exists
    void remove(UUID messageId, String emoji);             // force remove current user's reaction
    List<ChatReactionDto> listForMessage(UUID messageId);
    Map<String, Long> countsForMessage(UUID messageId);
}

