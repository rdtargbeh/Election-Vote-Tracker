package Backend.ElectionVote.dto;


import Backend.ElectionVote.enums.DiscrepancyStatus;

import java.util.UUID;

public record DiscrepancySearchRequest(
        UUID electionId,
        UUID countyId,
        UUID districtId,
        UUID centerId,
        DiscrepancyStatus status
) {}
