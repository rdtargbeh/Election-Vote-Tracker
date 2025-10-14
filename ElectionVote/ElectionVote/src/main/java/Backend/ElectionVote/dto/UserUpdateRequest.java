package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @NotBlank @Size(max = 30)
    private String firstName;

    @NotBlank @Size(max = 30)
    private String lastName;

    @NotBlank @Email  @Size(max = 50)
    private String userName;

    @NotBlank @Size(min = 3, max = 30)
    private String email;

    @Size(max = 20)
    private String phoneNumber;

    private Boolean active;
    private Boolean verified;

    @NotNull
    private RoleName roleName;
    private UUID partyId;                    // optional
    private UUID assignedCountyId;           // optional
    private UUID defaultOrgId;

}
