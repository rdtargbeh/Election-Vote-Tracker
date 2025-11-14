package Backend.ElectionVote.controller;

import Backend.ElectionVote.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final TokenService tokens;

    public record LoginRequest(String username, String password) {}


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.username() == null || req.password() == null) {
            throw new BadCredentialsException("Invalid credentials");
        }

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        // At this point user is authenticated by your UserDetailsService.
        // Derive a UUID userId from your principal or username. If your usernames are emails, you likely map them to a DB userId.
        UUID userId = extractUserId(auth); // implement below as needed
        String username = auth.getName();
        boolean isSystemAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equalsIgnoreCase(a.getAuthority()));

        String token = tokens.issueAccessToken(userId, username, isSystemAdmin);
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }

    private UUID extractUserId(Authentication auth) {
        // If your principal exposes getUserId(): reflect or cast and return it.
        // Fallback: derive UUID from username if you use UUID usernames, otherwise look it up from DB.
        try {
            var m = auth.getPrincipal().getClass().getMethod("getUserId");
            Object v = m.invoke(auth.getPrincipal());
            if (v instanceof UUID u) return u;
            if (v instanceof String s) return UUID.fromString(s);
        } catch (Exception ignored) {}
        // LAST RESORT ONLY (replace with real DB lookup for your users):
        try { return UUID.fromString(auth.getName()); } catch (Exception e) { return UUID.nameUUIDFromBytes(auth.getName().getBytes()); }
    }
}