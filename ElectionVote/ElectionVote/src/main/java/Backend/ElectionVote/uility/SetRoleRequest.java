package Backend.ElectionVote.uility;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Backend/ElectionVote/orgmember/dto/SetRoleRequest.java

@Getter
@Setter
public class SetRoleRequest {

    @NotBlank
    private String roleName;
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
