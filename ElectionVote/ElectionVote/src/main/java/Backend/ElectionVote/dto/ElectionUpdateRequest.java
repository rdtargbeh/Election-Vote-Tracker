package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.ElectionType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ElectionUpdateRequest {
    @Size(max = 100)
    private String electionName;
    @NotNull
    @Min(1900)
    private Integer year;
    @NotNull
    private ElectionType electionType;
    @NotNull
    private Boolean isActive;
}
