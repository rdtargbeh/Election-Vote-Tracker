package Backend.ElectionVote.security;

import Backend.ElectionVote.utility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TokenService {

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
                .map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
        boolean isSystemAdmin = roles.contains("ROLE_SYSTEM_ADMIN");
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer).issuedAt(now).expiresAt(exp)
                .subject(auth.getName()).claim("roles", roles).claim("isSystemAdmin", isSystemAdmin);
        // optionally add userId and orgId if present...
        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }


//    private final JwtEncoder encoder;
//    private final CurrentUserProvider currentUserProvider;
//
//    @Value("${app.security.issuer:https://vote-tracker.local}")
//    private String issuer;
//
//    @Value("${app.security.access-token-ttl:3600}")
//    private long accessTokenTtlSeconds;
//
//    public long expiresInSeconds() {
//        return accessTokenTtlSeconds;
//    }
//
//    public String mintAccessToken(Authentication auth) {
//        Instant now = Instant.now();
//        Instant exp = now.plusSeconds(accessTokenTtlSeconds);
//
//        Set<String> roles = auth.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .collect(Collectors.toUnmodifiableSet());
//
//        boolean isSystemAdmin = roles.contains("ROLE_SYSTEM_ADMIN");
//        UUID userId = currentUserProvider.currentUserId();
//
//        String orgId = TenantContext.get() != null
//                ? TenantContext.get().orgId().map(UUID::toString).orElse(null)
//                : null;
//
//        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
//                .issuer(issuer)
//                .issuedAt(now)
//                .expiresAt(exp)
//                .subject(auth.getName())
//                .claim("roles", roles)
//                .claim("isSystemAdmin", isSystemAdmin);
//
//        if (userId != null) claims.claim("userId", userId.toString());
//        if (orgId != null)  claims.claim("orgId", orgId);
//
//        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
//    }
}

