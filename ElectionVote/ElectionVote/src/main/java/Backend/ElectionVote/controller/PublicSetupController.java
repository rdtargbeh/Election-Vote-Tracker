package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.OrganizationCreateRequest;
import Backend.ElectionVote.dto.OrganizationDto;
import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.service.SystemUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/setup")
@RequiredArgsConstructor
public class PublicSetupController {

    private final OrganizationService organizationService;
    private final SystemUserService systemUserService;
    private final SystemUserRepository users;

    /** Only allowed if no users exist yet (true first-run bootstrap). */
    private void assertBootstrapOpen() {
        if (users.count() > 0) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bootstrap closed: platform already initialized");
        }
    }

    @PostMapping("/create-org")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDto createOrg(@RequestBody OrganizationCreateRequest req) {
        assertBootstrapOpen();
        return organizationService.create(req);
    }

    @PostMapping("/create-system-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createSystemAdmin(@RequestBody UserCreateRequest req) {
        assertBootstrapOpen();
        return systemUserService.createPlatformAdmin(req);
    }
}