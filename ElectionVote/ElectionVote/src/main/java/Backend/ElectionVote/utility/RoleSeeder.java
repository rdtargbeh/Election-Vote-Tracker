package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.UserRole;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * Seed user roles. Mark core/builtin roles as isBuiltin=true so they are protected.
 */
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {
    private final UserRoleRepository roles;

    @Override
    public void run(String... args) {
        for (RoleName rn : RoleName.values()) {
            roles.findByRoleName(rn).orElseGet(() -> {
                // Mark common core roles builtin to prevent deletion
                boolean builtin = switch (rn) {
                    case SYSTEM_ADMIN, NEC_ADMIN, ADMIN, PARTY_ADMIN, AGENT, OBSERVER, SUPERVISOR, COORDINATOR, DATA_ENTRY, AUDITOR -> true;
                    default -> false;
                };
                return roles.save(UserRole.builder()
                        .roleName(rn)
                        .description(rn.name())
                        .isBuiltin(builtin)
                        .build());
            });
        }
    }
}


//// Backend/ElectionVote/bootstrap/RoleSeeder.java
//@Component
//@RequiredArgsConstructor
//public class RoleSeeder implements CommandLineRunner {
//    private final UserRoleRepository roles;
//
//    @Override public void run(String... args) {
//        for (RoleName rn : RoleName.values()) {
//            roles.findByRoleName(rn).orElseGet(() ->
//                    roles.save(UserRole.builder().roleName(rn).description(rn.name()).build())
//            );
//        }
//    }
//}
