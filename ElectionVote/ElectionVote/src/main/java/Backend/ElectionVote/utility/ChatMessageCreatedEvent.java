package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.Organization;
import java.util.UUID;


public record ChatMessageCreatedEvent(
        UUID roomId,
        UUID messageId,
        UUID senderUserId,
        Organization organization,
        String roomDisplayName // may be null for DMs
) {}