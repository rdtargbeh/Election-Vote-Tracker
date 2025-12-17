package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRoleRequest {

    @NotNull
    private RoleName roleName;

    @Size(max = 150)
    private String description;
}
