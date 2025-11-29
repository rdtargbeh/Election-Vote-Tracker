package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.ChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatReactionRepository extends JpaRepository<ChatReaction, UUID> {

    Optional<ChatReaction> findByMessage_MessageIdAndUser_UserIdAndEmoji(UUID messageId, UUID userId, String emoji);

    List<ChatReaction> findByMessage_MessageId(UUID messageId);

    List<ChatReaction> findByMessage_MessageIdAndEmoji(UUID messageId, String emoji);

    long countByMessage_MessageIdAndEmoji(UUID messageId, String emoji);

    long countByMessage_MessageId(UUID messageId);
}

