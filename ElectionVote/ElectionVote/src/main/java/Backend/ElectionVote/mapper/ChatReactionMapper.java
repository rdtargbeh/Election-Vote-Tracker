package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.ChatReactionDto;
import Backend.ElectionVote.entity.ChatReaction;

import java.util.Optional;

public class ChatReactionMapper {

    public ChatReactionDto toDTO(ChatReaction r) {
        if (r == null) return null;

        String fn = Optional.ofNullable(r.getUser().getFirstName()).orElse("");
        String ln = Optional.ofNullable(r.getUser().getLastName()).orElse("");

        return ChatReactionDto.builder()
                .reactionId(r.getReactionId())
                .messageId(r.getMessage().getMessageId())
                .userId(r.getUser().getUserId())
                .userName((fn + " " + ln).trim())
                .emoji(r.getEmoji())
                .dateCreated(r.getDateCreated())
                .build();
    }
}
