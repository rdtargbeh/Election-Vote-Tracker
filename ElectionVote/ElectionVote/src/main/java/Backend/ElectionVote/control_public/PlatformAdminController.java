package Backend.ElectionVote.control_public;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.service.SystemUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformAdminController {


    private final AuthorizationService authz;
    private final OrganizationService organizationService;
    private final SystemUserService systemUserService;


    /**
     * Create a new organization (tenant).
     * Only SYSTEM_ADMIN (platform admin) can do this.
     */
    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDto createOrganization(@RequestBody @Valid OrganizationCreateRequest req) {
        authz.requirePlatformAdmin(); // uses TenantContext / token to ensure SYSTEM_ADMIN
        return organizationService.create(req);
    }


    /**
     * Create a global/system user (not bound to a specific org).
     *
     * - If req.roleName = SYSTEM_ADMIN → user.systemAdmin = true (platform owner).
     * - Other roles → global users, systemAdmin = false.
     */
    @PostMapping("/system-users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createSystemUser(@RequestBody @Valid UserCreateRequest req) {
        authz.requireAny("SYSTEM_ADMIN");
        return systemUserService.createPlatformAdmin(req);
    }



}
