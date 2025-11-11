package Backend.ElectionVote.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter
public class PollingCenterUpdateRequest {
    @Size(max = 150)
    private String centerName;

    @Size(max = 50)
    private String code;

    @Min(0)
    private Integer registeredVoters;

    private UUID districtId;
}