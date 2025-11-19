package Backend.ElectionVote.control_public;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.SystemUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/tenants/users")
@RequiredArgsConstructor
public class TenantUserController {

    private final AuthorizationService authz;
    private final SystemUserService systemUserService;


    /**
     * Create a tenant member in the current org (from TenantContext).
     *
     * Allowed roles (enforced in service):
     *   AGENT, SUPERVISOR, DATA_ENTRY, OBSERVER, COORDINATOR, AUDITOR
     *
     * Caller:
     *   PARTY_ADMIN, ADMIN, or SYSTEM_ADMIN in this tenant.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createMember(@RequestBody @Valid UserCreateRequest req) {
        // Caller must be a tenant PARTY_ADMIN, ADMIN, or platform SYSTEM_ADMIN
        authz.requireAny("PARTY_ADMIN", "ADMIN", "SYSTEM_ADMIN");
        return systemUserService.createTenantMemberRestricted(req);
    }


    /**
     * Create a tenant admin for the current org (from TenantContext).
     *
     * Allowed roles (enforced in service):
     *   ADMIN or PARTY_ADMIN
     *
     * Caller:
     *   SYSTEM_ADMIN (platform admin) – must send X-Org-Id for the target org.
     *
     * Usage:
     *   - SYSTEM_ADMIN sets X-Org-Id=<orgId> and roleName=PARTY_ADMIN to create the first PARTY_ADMIN
     *     for that organization.
     */
    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createTenantAdmin(@RequestBody @Valid UserCreateRequest req) {
        // Platform-level check: does NOT depend on tenant membership
        authz.requirePlatformAdmin();
        return systemUserService.createTenantAdmin(req);
    }




//      00089918-4d00-42f3-9e40-3319c4cbb93c   UP
//      465f2d59-53d7-4ce4-9d20-97cd30eda56d   CDC
//      c1709ad1-948e-45e4-8aef-19f5bde01428   NEC

    // X-Org-Id: 00089918-4d00-42f3-9e40-3319c4cbb93c

}
