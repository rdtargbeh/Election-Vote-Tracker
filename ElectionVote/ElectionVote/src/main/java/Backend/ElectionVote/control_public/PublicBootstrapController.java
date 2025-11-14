package Backend.ElectionVote.control_public;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.service.SystemUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/bootstrap")
@RequiredArgsConstructor
public class PublicBootstrapController {
    private final SystemUserRepository users;
    private final SystemUserService systemUserService;

    private void assertBootstrapOpen() {
        if (users.count() > 0) {
            throw new org.springframework.security.access.AccessDeniedException("Bootstrap closed");
        }
    }

    @PostMapping("/system-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createFirstSystemAdmin(@RequestBody @Valid UserCreateRequest req) {
        assertBootstrapOpen();
        return systemUserService.createPlatformAdmin(req); // sets isSystemAdmin = true
    }
}
