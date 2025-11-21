package Backend.ElectionVote.controller;


import Backend.ElectionVote.security.TokenService;
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



@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final TokenService tokenService;

    // This is the DTO Jackson will bind to
    public record LoginRequest(String userName, String password) { }

    public record TokenResponse(String access_token, long expires_in, String token_type) { }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {

        System.out.println("Login attempt identifier=" + req.userName()
                + ", passwordNull=" + (req.password() == null));


        // identifier = email or username; AuthUserDetailsService already supports both
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.userName(), req.password())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = tokenService.mintAccessToken(auth);
        return ResponseEntity.ok(new AuthResponse(jwt, tokenService.expiresInSeconds()));
    }

    @Getter
    @AllArgsConstructor
    static class AuthResponse {
        private String accessToken;
        private long expiresIn;
    }

}



