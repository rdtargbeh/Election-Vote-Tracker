package Backend.ElectionVote.control_public;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.SystemUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/tenants/users")
@RequiredArgsConstructor
public class TenantUserController {
    private final AuthorizationService authz;
    private final SystemUserService systemUserService;

    /** PARTY_ADMIN (or ADMIN / SYSTEM_ADMIN inside a tenant) can create members, but not ADMIN/SYSTEM_ADMIN. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createMember(@RequestBody @Valid UserCreateRequest req) {
        authz.requireAny("PARTY_ADMIN", "ADMIN", "SYSTEM_ADMIN");
        return systemUserService.createTenantMemberRestricted(req);
    }

    /** Optional: create tenant ADMIN (only platform admin may do this and must send X-Org-Id). */
    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createTenantAdmin(@RequestBody @Valid UserCreateRequest req) {
        authz.requireAny("SYSTEM_ADMIN");
        return systemUserService.createTenantAdmin(req);
    }
}
