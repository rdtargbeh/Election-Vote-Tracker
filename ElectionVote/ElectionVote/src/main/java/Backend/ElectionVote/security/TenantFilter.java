package Backend.ElectionVote.security;

import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.utility.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;


/**
 * Strict tenant resolution and binding for every authenticated request.
 *
 * Rules:
 *  1) Accept tenant via subdomain or "X-Org-Id" header.
 *  2) If BOTH present, they MUST match → 400.
 *  3) Only ACTIVE organizations are accepted → 400 otherwise.
 *  4) isSystemAdmin derived from SecurityContext roles/claims.
 *
 * This filter DOES NOT write to MDC. RequestContextMdcFilter is the single MDC writer.
 *
 * ORDERING (required):
 *   Add this filter to the Spring Security chain AFTER authentication, e.g.:
 *     http.addFilterAfter(tenantFilter, org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter.class);
 *   (or UsernamePasswordAuthenticationFilter for form-login chains)
 */
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Org-Id";
    private final ObjectProvider<OrganizationRepository> organizationsProvider;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String m = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(m)) return true;

        String p = request.getRequestURI();

        // ✅ Public endpoints that should NOT require a tenant
        if (p.startsWith("/api/public/")
                || p.startsWith("/actuator")
                || p.startsWith("/favicon")
                || p.startsWith("/assets")
                || p.startsWith("/static")
                || p.startsWith("/v3/api-docs")
                || p.startsWith("/swagger")
                || p.startsWith("/swagger-ui")) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 🟢 If this request is public, just pass through
        if (shouldNotFilter(req)) {
            chain.doFilter(req, res);
            return;
        }

        // 🔒 For protected APIs, a tenant is required
        UUID orgId = resolveTenantStrict(req); // throws BadTenantSelectionException if not present/valid
        boolean isSystemAdmin = resolveSystemAdmin();

        try {
            TenantContext.set(null, orgId, isSystemAdmin);
            if (orgId != null) MDC.put("orgId", orgId.toString());
            MDC.put("isSystemAdmin", Boolean.toString(isSystemAdmin));
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            MDC.remove("orgId");
            MDC.remove("isSystemAdmin");
        }
    }



//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        // Skip preflight/static/health/error paths
//        final String method = request.getMethod();
//        if ("OPTIONS".equalsIgnoreCase(method)) return true;
//
//        final String path = request.getRequestURI();
//        return path.startsWith("/public/")
//                || path.startsWith("/actuator")
//                || path.startsWith("/favicon")
//                || path.startsWith("/assets")
//                || path.startsWith("/static")
//                || path.startsWith("/error");
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
//            throws ServletException, IOException {
//
//        UUID orgId = resolveTenantStrict(req);
//        boolean isSystemAdmin = resolveSystemAdmin();
//
//        try {
//            // userId is optional here; set if you derive it elsewhere
//            TenantContext.set(/* userId */ null, orgId, isSystemAdmin);
//            chain.doFilter(req, res);
//        } finally {
//            TenantContext.clear();
//        }
//    }

    /**
     * Resolve tenant using header and subdomain with strict conflict checks.
     * Fails fast with 400 if invalid/missing when tenancy is required.
     */
    private UUID resolveTenantStrict(HttpServletRequest req) {
        OrganizationRepository repo = organizationsProvider.getIfAvailable();
        if (repo == null) throw badRequest("Tenant resolution unavailable");

        UUID fromHeader = parseUuidOrNull(req.getHeader(TENANT_HEADER));
        if (fromHeader != null && !repo.existsByOrgIdAndIsActiveTrue(fromHeader)) {
            throw badRequest("Unknown or inactive organization (header)");
        }

        String sub = deriveSubdomain(req.getServerName());
        UUID fromSub = null;
        if (sub != null) {
            fromSub = repo.findIdBySubdomainIgnoreCaseAndIsActiveTrue(sub)
                    .orElseThrow(() -> badRequest("Unknown or inactive organization (subdomain)"));
        }

        if (fromHeader != null && fromSub != null && !fromHeader.equals(fromSub)) {
            throw badRequest("Header X-Org-Id conflicts with subdomain tenant");
        }

        UUID resolved = (fromHeader != null) ? fromHeader : fromSub;
        if (resolved == null) {
            // Protected APIs should always present a tenant
            throw badRequest("Organization is required (X-Org-Id header or tenant subdomain)");
        }
        return resolved;
    }

    private boolean resolveSystemAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) return false;

        // Role-based
        boolean byRole = a.getAuthorities().stream()
                .anyMatch(au -> "ROLE_SYSTEM_ADMIN".equalsIgnoreCase(au.getAuthority()));
        if (byRole) return true;

        // Claim-based (e.g., JWT)
        Object principal = a.getPrincipal();
        try {
            var m = principal.getClass().getMethod("getClaims");
            Object claimsObj = m.invoke(principal);
            if (claimsObj instanceof Map<?,?> claims) {
                Object v = claims.get("isSystemAdmin");
                if (v instanceof Boolean b) return b;
                if (v instanceof String s) return Boolean.parseBoolean(s);
            }
        } catch (Exception ignored) { /* principal has no getClaims() */ }

        if (principal instanceof Map<?,?> claims) {
            Object v = claims.get("isSystemAdmin");
            if (v instanceof Boolean b) return b;
            if (v instanceof String s) return Boolean.parseBoolean(s);
        }
        return false;
    }

    private static UUID parseUuidOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s.trim()); } catch (Exception e) { return null; }
    }

    private static String deriveSubdomain(String host) {
        if (host == null || host.isBlank()) return null;
        String[] labels = host.split("\\.");
        if (labels.length >= 3) {
            String first = labels[0];
            if (!"www".equalsIgnoreCase(first)) return first;
        }
        return null;
    }

    // 400 exception; map it in a @RestControllerAdvice to a JSON error
    private static BadTenantSelectionException badRequest(String msg) {
        return new BadTenantSelectionException(msg);
    }

    public static class BadTenantSelectionException extends RuntimeException {
        public BadTenantSelectionException(String message) { super(message); }
    }
}



///**
// * Resolves current tenant (Organization) for the request.
// *
// * Order: runs very early so downstream security/service layers can rely on TenantContext.
// *
// * Resolution strategy:
// *  1) Header "X-Org-Id" (UUID)
// *  2) (optional) Subdomain (e.g., cdc.example.com -> "cdc" -> lookup Organization.subdomain)
// *
// * If neither is found, the request proceeds WITHOUT a tenant; tenant-scoped services will throw.
// * You can flip FAIL_ON_MISSING to true to return 400 for protected APIs.
// */
//
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE + 10)
//@RequiredArgsConstructor
//public class TenantFilter extends OncePerRequestFilter {
//
//    private static final String TENANT_HEADER = "X-Org-Id";
//    @Autowired
//    private ObjectProvider<OrganizationRepository> organizationsProvider;
//
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String m = request.getMethod();
//        if ("OPTIONS".equalsIgnoreCase(m)) return true;
//        String p = request.getRequestURI();
//        return p.startsWith("/public/") || p.startsWith("/actuator")
//                || p.startsWith("/favicon") || p.startsWith("/assets") || p.startsWith("/static");
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
//            throws ServletException, IOException {
//
//        UUID orgId = resolveOrg(req);
//        boolean isSystemAdmin = resolveSystemAdmin();
//
//        try {
//            // set userId here only if you parse it from SecurityContext/JWT; null is fine too.
//            TenantContext.set(null, orgId, isSystemAdmin);
//            if (orgId != null) MDC.put("orgId", orgId.toString());
//            MDC.put("isSystemAdmin", Boolean.toString(isSystemAdmin));
//            chain.doFilter(req, res);
//        } finally {
//            TenantContext.clear();
//            MDC.remove("orgId");
//            MDC.remove("isSystemAdmin");
//        }
//    }
//
//    private UUID resolveOrg(HttpServletRequest req) {
//        UUID fromHeader = parseUuidOrNull(req.getHeader(TENANT_HEADER));
//        if (fromHeader != null) {
//            OrganizationRepository repo = organizationsProvider.getIfAvailable();
//            return (repo != null && repo.existsByOrgIdAndIsActiveTrue(fromHeader)) ? fromHeader : null;
//        }
//        String sub = deriveSubdomain(req.getServerName());
//        if (sub != null) {
//            OrganizationRepository repo = organizationsProvider.getIfAvailable();
//            if (repo != null) {
//                return repo.findIdBySubdomainIgnoreCaseAndIsActiveTrue(sub).orElse(null);
//            }
//        }
//        return null;
//    }
//
//    private boolean resolveSystemAdmin() {
//        Authentication a = SecurityContextHolder.getContext().getAuthentication();
//        if (a == null || !a.isAuthenticated()) return false;
//        if (a.getAuthorities().stream().anyMatch(au -> "ROLE_SYSTEM_ADMIN".equalsIgnoreCase(au.getAuthority()))) return true;
//
//        Object principal = a.getPrincipal();
//        try {
//            var m = principal.getClass().getMethod("getClaims");
//            Object claimsObj = m.invoke(principal);
//            if (claimsObj instanceof Map<?,?> claims) {
//                Object v = claims.get("isSystemAdmin");
//                if (v instanceof Boolean b) return b;
//                if (v instanceof String s) return Boolean.parseBoolean(s);
//            }
//        } catch (Exception ignored) {}
//        if (principal instanceof Map<?,?> claims) {
//            Object v = claims.get("isSystemAdmin");
//            if (v instanceof Boolean b) return b;
//            if (v instanceof String s) return Boolean.parseBoolean(s);
//        }
//        return false;
//    }
//
//    private UUID parseUuidOrNull(String s) {
//        if (s == null || s.isBlank()) return null;
//        try { return UUID.fromString(s.trim()); } catch (Exception e) { return null; }
//    }
//
//    private String deriveSubdomain(String host) {
//        if (host == null || host.isBlank()) return null;
//        String[] labels = host.split("\\.");
//        if (labels.length >= 3) {
//            String first = labels[0];
//            if (!"www".equalsIgnoreCase(first)) return first;
//        }
//        return null;
//    }
//}