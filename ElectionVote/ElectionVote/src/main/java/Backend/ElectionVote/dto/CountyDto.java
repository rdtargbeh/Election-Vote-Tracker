package Backend.ElectionVote.dto;

import java.util.UUID;

public record CountyDto(
        UUID countyId,
        String countyName
) {}