package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.UserRole;
import Backend.ElectionVote.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    Optional<UserRole> findByRoleName(RoleName roleName);
    boolean existsByRoleName(RoleName roleName);

    long countByRoleId(UUID roleId);

}
