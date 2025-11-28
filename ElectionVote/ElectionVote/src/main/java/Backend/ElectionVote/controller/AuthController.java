package Backend.ElectionVote.controller;


import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.security.TokenService;
import Backend.ElectionVote.service.AuditLogService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthenticationManager authManager;
    private final TokenService tokenService;
    private final SystemUserRepository systemUserRepository; // <-- add this
    private final AuditLogService auditLogService;

    // Request DTO
    public record LoginRequest(String userName, String password) { }



    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {

        System.out.println("Login attempt identifier=" + req.userName()
                + ", passwordNull=" + (req.password() == null));

        // 1) Authenticate
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.userName(), req.password())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        // 2) Mint JWT
        String jwt = tokenService.mintAccessToken(auth);

        // 3) Resolve the actual user record (username OR email, case-insensitive)
        String identifier = auth.getName(); // Spring Security principal name

        SystemUser user =
                systemUserRepository.findByUserNameIgnoreCase(identifier)
                        .or(() -> systemUserRepository.findByEmailIgnoreCase(identifier))
                        .orElseThrow(() -> new IllegalStateException(
                                "User not found after successful authentication: " + identifier));

        // 4) Extract default organization (if any)
        Organization defaultOrg = user.getDefaultOrg();

        UUID defaultOrgId = null;
        String defaultOrgName = null;

        if (defaultOrg != null) {
            defaultOrgId = defaultOrg.getOrgId();     // adjust if your getter names differ
            defaultOrgName = defaultOrg.getOrgName(); // adjust if needed
        }

        // 5) Return enriched response
        return ResponseEntity.ok(
                new AuthResponse(jwt, tokenService.expiresInSeconds(), defaultOrgId, defaultOrgName)
        );
    }


// Response DTO
@Getter
@AllArgsConstructor
static class AuthResponse {
    private String accessToken;
    private long expiresIn;
    private UUID defaultOrgId;
    private String defaultOrgName;
}

}



