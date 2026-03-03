package Backend.ElectionVote.utility;

import lombok.Data;
import java.util.UUID;

/** Request to open (or create) a DM with another user in the same tenant */
@Data
public class OpenDmRequest {
    private UUID otherUserId;
}