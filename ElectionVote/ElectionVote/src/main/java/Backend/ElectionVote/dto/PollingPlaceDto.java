package Backend.ElectionVote.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PollingPlaceDto {

    private UUID placeId;

    private UUID centerId;
    private String centerCode;
    private String centerName;

    // District
    private UUID districtId;
    private String districtName;

    // County
    private UUID countyId;
    private String countyName;

    // Place
    private Integer placeNumber;
    private String code;
    private String label;

    private boolean active;
}