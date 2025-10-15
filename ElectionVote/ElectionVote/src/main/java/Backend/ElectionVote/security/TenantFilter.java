package Backend.ElectionVote.security;

import Backend.ElectionVote.repository.OrganizationRepository;
import Backend.ElectionVote.uility.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
    private static final boolean FAIL_ON_MISSING = false; // set true if you want early 400 for tenant APIs

    private final OrganizationRepository organizations;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip for CORS preflight and optionally for public endpoints
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        String path = request.getRequestURI();
        // Allow public endpoints without tenant (adjust as needed)
        if (path.startsWith("/public/") || path.startsWith("/actuator")) return true;
        // static resources
        if (path.startsWith("/favicon") || path.startsWith("/assets") || path.startsWith("/static")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        UUID resolved = null;
        try {
            // 1) Prefer explicit header
            String orgHeader = request.getHeader(TENANT_HEADER);

            if (orgHeader != null && !orgHeader.isBlank()) {
                try {
                    UUID orgId = UUID.fromString(orgHeader.trim());
                    // Ensure org exists and is active
                    boolean exists = organizations.existsById(orgId);
                    if (exists) {
                        resolved = orgId;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID; fall through to subdomain (if enabled)
                }
            }

            // 2) Optional: derive from subdomain if header missing/invalid
            if (resolved == null) {
                String host = request.getServerName(); // e.g., cdc.app.example.com
                String sub = deriveSubdomain(host);
                if (sub != null) {
                    resolved = organizations.findBySubdomainIgnoreCase(sub)
                            .filter(o -> o.isActive()) // only active tenants
                            .map(o -> o.getOrgId())
                            .orElse(null);
                }
            }

            if (resolved != null) {
                TenantContext.set(resolved);
                MDC.put("orgId", resolved.toString()); // useful in logs
            } else if (FAIL_ON_MISSING) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Org-Id is required");
                return;
            }

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            MDC.remove("orgId");
        }
    }

    /**
     * Extract subdomain from host. Customize for your domain shape.
     * Examples:
     *  - "cdc.example.com" -> "cdc"
     *  - "app.internal.local" -> null (if you only use header in non-subdomain envs)
     */
    private String deriveSubdomain(String host) {
        if (host == null || host.isBlank()) return null;
        // naive split: take first label if there are 3+ labels (sub + domain + tld)
        String[] labels = host.split("\\.");
        if (labels.length >= 3) {
            String first = labels[0];
            // optionally ignore "www"
            if ("www".equalsIgnoreCase(first)) return null;
            return first;
        }
        return null;
    }


}