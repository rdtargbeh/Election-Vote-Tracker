package Backend.ElectionVote.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;
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

    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
}