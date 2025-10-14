package Backend.ElectionVote.mapper;

import Backend.ElectionVote.dto.UserCreateRoleRequest;
import Backend.ElectionVote.dto.UserRoleDto;
import Backend.ElectionVote.dto.UserUpdateRoleRequest;
import Backend.ElectionVote.entity.UserRole;

public class UserRoleMapper {

    public UserRoleDto toDTO(UserRole role){
        if (role == null) return null;
        return UserRoleDto.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .build();
    }

    public UserRole toEntity(UserCreateRoleRequest req){
        if (req == null) return null;
        return UserRole.builder()
                .roleName(req.getRoleName())
                .description(req.getDescription())
                .build();
    }

    public void apply(UserUpdateRoleRequest req, UserRole role){
        if (req == null || role == null) return;
        if (req.getDescription() != null) role.setDescription(req.getDescription());
    }

}
