package Backend.ElectionVote.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NecResultGeoDto {
    private UUID resultId;
    private UUID electionId;
    private UUID countyId;   private String countyName;
    private UUID districtId; private String districtName;
    private UUID centerId;   private String centerCode; private String centerName;
    private Integer ballotsCast; private Integer totalRegisteredVoters;
    private String candidateVotesJson; // keep as JSON string; frontend can parse
    private LocalDateTime uploadTime;  private String source;
}
