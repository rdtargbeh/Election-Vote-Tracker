package Backend.ElectionVote.security;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final String issuer;

    public TokenService(JwtEncoder encoder,
                        @Value("${app.security.issuer:https://vote-tracker.local}") String issuer) {
        this.encoder = encoder;
        this.issuer = issuer;
    }

    /**
     * Issue a short-lived access token for the authenticated user.
     * Claims kept minimal; roles can be added if you want client-side decisions.
     */
    public String issueAccessToken(UUID userId, String username, boolean isSystemAdmin) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(60, ChronoUnit.MINUTES))
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("preferred_username", username)
                .claim("isSystemAdmin", isSystemAdmin)
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}