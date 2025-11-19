package Backend.ElectionVote.security;

import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.repository.SystemUserRepository;
import Backend.ElectionVote.utility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TokenService {

    @Autowired
    private SystemUserRepository users;
    private final JwtEncoder encoder;

    @Value("${app.security.issuer:https://vote-tracker.local}")
    private String issuer;

    @Value("${app.security.access-token-ttl:3600}")
    private long accessTokenTtlSeconds;


    public long expiresInSeconds() { return accessTokenTtlSeconds; }

    public String mintAccessToken(Authentication auth) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenTtlSeconds);

        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        boolean isSystemAdmin = roles.contains("ROLE_SYSTEM_ADMIN");

        String userId = extractUserId(auth); // safe fallback

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(userId) // subject can be UUID or username/email
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("isSystemAdmin", isSystemAdmin);

        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }


    private String extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();

        // Try UserDetails
        if (principal instanceof UserDetails ud) {
            String name = ud.getUsername();
            try {
                return UUID.fromString(name).toString(); // if username is UUID
            } catch (Exception ignored) {
                return name; // fallback to username/email
            }
        }

        // Try reflective getters
        try {
            Method m = principal.getClass().getMethod("getUserId");
            Object v = m.invoke(principal);
            if (v instanceof UUID u) return u.toString();
            if (v instanceof String s) {
                try { return UUID.fromString(s).toString(); } catch (Exception ignored) {
                    return s; // fallback to raw string
                }
            }
        } catch (Exception ignored) {}

        // Fallback to auth.getName()
        String name = auth.getName();
        return (name != null && !name.isBlank()) ? name : "unknown";
    }



}

