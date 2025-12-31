package Backend.ElectionVote.utility;

import Backend.ElectionVote.enums.MfaMethod;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaStatusResponse {
    private boolean enabled;
    private MfaMethod method;
}
