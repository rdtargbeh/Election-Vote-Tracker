package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.ContestOptionType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContestOptionUpdateRequest {

    // usually don't change contestId in production; we will block it in service unless you want it
    private UUID contestId;
    private UUID electionId;

    private ContestOptionType optionType;

    private UUID electId;
    private String optionLabel;

    private Integer optionOrder;

    private Boolean isActive;
}
