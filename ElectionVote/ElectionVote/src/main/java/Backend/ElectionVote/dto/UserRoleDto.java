package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.RoleName;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleDto {
    private UUID roleId;
    private RoleName roleName;
    private String description;
}