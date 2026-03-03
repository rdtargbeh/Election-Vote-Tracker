package Backend.ElectionVote.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter
public class PollingCenterCreateRequest {
    @NotBlank @Size(max = 150)
    private String centerName;

//    @NotBlank @Size(max = 50)
//    private String code;

    @Min(0)
    private int registeredVoters;

    @NotNull
    private UUID districtId;
}