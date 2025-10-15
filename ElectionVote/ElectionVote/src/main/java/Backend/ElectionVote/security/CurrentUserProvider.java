package Backend.ElectionVote.security;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

public interface CurrentUserProvider {
    UUID currentUserId();
    boolean isAuthenticated();

    static CurrentUserProvider springSecurity() {
        return new CurrentUserProvider() {
            @Override
            public UUID currentUserId() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) return null;

                // 1) Custom principal with getUserId()
                Object principal = auth.getPrincipal();
                UUID id = extractFromCustomPrincipal(principal);
                if (id != null) return id;

                // 2) If principal is a Spring Jwt (no need for JwtAuthenticationToken)
                //    Works when principal is org.springframework.security.oauth2.jwt.Jwt
                id = extractFromJwtPrincipal(principal);
                if (id != null) return id;

                // 3) If principal is a Map of claims
                if (principal instanceof Map<?,?> claims) {
                    id = extractFromClaimsMap(claims);
                    if (id != null) return id;
                }

                // 4) Fallback: try auth.getName() as UUID
                try { return UUID.fromString(auth.getName()); } catch (Exception ignored) {}
                return null;
            }

            @Override
            public boolean isAuthenticated() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                return auth != null && auth.isAuthenticated();
            }

            /* ---------------- helpers ---------------- */

            private UUID extractFromCustomPrincipal(Object principal) {
                // Example: your own UserPrincipal with getUserId()
                try {
                    if (principal != null) {
                        var m = principal.getClass().getMethod("getUserId");
                        Object v = m.invoke(principal);
                        if (v instanceof UUID u) return u;
                        if (v instanceof String s) return UUID.fromString(s);
                    }
                } catch (Exception ignored) {}
                return null;
            }

            @SuppressWarnings("unchecked")
            private UUID extractFromJwtPrincipal(Object principal) {
                try {
                    // Avoid compile-time dependency on Jwt:
                    // reflectively call getClaims() if the principal has it
                    var method = principal.getClass().getMethod("getClaims");
                    Object claimsObj = method.invoke(principal);
                    if (claimsObj instanceof Map<?,?> claims) {
                        return extractFromClaimsMap(claims);
                    }
                } catch (Exception ignored) {}
                return null;
            }

            @SuppressWarnings("unchecked")
            private UUID extractFromClaimsMap(Map<?, ?> claims) {
                Object v = claims.get("userId");
                if (v == null) v = claims.get("sub");   // <- safe two-step, no getOrDefault
                if (v instanceof String s) {
                    try { return UUID.fromString(s); } catch (Exception ignored) {}
                }
                return null;
            }
        };
    }
}
