package Backend.ElectionVote.uility;

import Backend.ElectionVote.entity.UserRole;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Backend/ElectionVote/bootstrap/RoleSeeder.java
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {
    private final UserRoleRepository roles;

    @Override public void run(String... args) {
        for (RoleName rn : RoleName.values()) {
            roles.findByRoleName(rn).orElseGet(() ->
                    roles.save(UserRole.builder().roleName(rn).description(rn.name()).build())
            );
        }
    }
}
