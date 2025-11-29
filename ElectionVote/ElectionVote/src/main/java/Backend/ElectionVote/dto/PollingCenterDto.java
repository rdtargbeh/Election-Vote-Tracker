package Backend.ElectionVote.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder
public class PollingCenterDto {
    private UUID centerId;
    private String centerName;
    private String code;
//    private int registeredVoters;

    private UUID districtId;
    private String districtName;
    private UUID countyId;
    private String countyName;
}