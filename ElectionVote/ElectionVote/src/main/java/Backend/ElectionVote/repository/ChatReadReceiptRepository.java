package Backend.ElectionVote.repository;


import Backend.ElectionVote.entity.ChatReadReceipt;
import Backend.ElectionVote.utility.ChatReadReceiptId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatReadReceiptRepository extends JpaRepository<ChatReadReceipt, ChatReadReceiptId> {

    Optional<ChatReadReceipt> findByMessage_MessageIdAndUser_UserId(UUID messageId, UUID userId);

    List<ChatReadReceipt> findByMessage_MessageId(UUID messageId);

    long countByMessage_MessageId(UUID messageId);

    boolean existsByMessage_MessageIdAndUser_UserId(UUID messageId, UUID userId);
}
