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
 * Resolves current tenant (Organization) for the request.
 *
 * Order: runs very early so downstream security/service layers can rely on TenantContext.
 *
 * Resolution strategy:
 *  1) Header "X-Org-Id" (UUID)
 *  2) (optional) Subdomain (e.g., cdc.example.com -> "cdc" -> lookup Organization.subdomain)
 *
 * If neither is found, the request proceeds WITHOUT a tenant; tenant-scoped services will throw.
 * You can flip FAIL_ON_MISSING to true to return 400 for protected APIs.
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Org-Id";
    @Autowired
    private ObjectProvider<OrganizationRepository> organizationsProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String m = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(m)) return true;
        String p = request.getRequestURI();
        return p.startsWith("/public/") || p.startsWith("/actuator")
                || p.startsWith("/favicon") || p.startsWith("/assets") || p.startsWith("/static");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        UUID orgId = resolveOrg(req);
        boolean isSystemAdmin = resolveSystemAdmin();

        try {
            // set userId here only if you parse it from SecurityContext/JWT; null is fine too.
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

    private UUID resolveOrg(HttpServletRequest req) {
        UUID fromHeader = parseUuidOrNull(req.getHeader(TENANT_HEADER));
        if (fromHeader != null) {
            OrganizationRepository repo = organizationsProvider.getIfAvailable();
            return (repo != null && repo.existsByOrgIdAndIsActiveTrue(fromHeader)) ? fromHeader : null;
        }
        String sub = deriveSubdomain(req.getServerName());
        if (sub != null) {
            OrganizationRepository repo = organizationsProvider.getIfAvailable();
            if (repo != null) {
                return repo.findIdBySubdomainIgnoreCaseAndIsActiveTrue(sub).orElse(null);
            }
        }
        return null;
    }

    private boolean resolveSystemAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) return false;
        if (a.getAuthorities().stream().anyMatch(au -> "ROLE_SYSTEM_ADMIN".equalsIgnoreCase(au.getAuthority()))) return true;

        Object principal = a.getPrincipal();
        try {
            var m = principal.getClass().getMethod("getClaims");
            Object claimsObj = m.invoke(principal);
            if (claimsObj instanceof Map<?,?> claims) {
                Object v = claims.get("isSystemAdmin");
                if (v instanceof Boolean b) return b;
                if (v instanceof String s) return Boolean.parseBoolean(s);
            }
        } catch (Exception ignored) {}
        if (principal instanceof Map<?,?> claims) {
            Object v = claims.get("isSystemAdmin");
            if (v instanceof Boolean b) return b;
            if (v instanceof String s) return Boolean.parseBoolean(s);
        }
        return false;
    }

    private UUID parseUuidOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s.trim()); } catch (Exception e) { return null; }
    }

    private String deriveSubdomain(String host) {
        if (host == null || host.isBlank()) return null;
        String[] labels = host.split("\\.");
        if (labels.length >= 3) {
            String first = labels[0];
            if (!"www".equalsIgnoreCase(first)) return first;
        }
        return null;
    }
}