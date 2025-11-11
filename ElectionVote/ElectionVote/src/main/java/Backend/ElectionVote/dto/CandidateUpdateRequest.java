package Backend.ElectionVote.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class CandidateUpdateRequest {
    private String fullName;           // null => no change
    @Size(max = 30)
    private String position;
    private UUID partyId;              // set new party if provided (see service logic)
    private String photoUrl;
    private Boolean isActive;          // boxed for partial update
}