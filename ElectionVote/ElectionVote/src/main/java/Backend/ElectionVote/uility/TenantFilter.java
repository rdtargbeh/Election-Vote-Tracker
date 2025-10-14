package Backend.ElectionVote.uility;

import Backend.ElectionVote.repository.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final OrganizationRepository organizations;

    public TenantFilter(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1) Prefer explicit header
            String orgHeader = request.getHeader("X-Org-Id");

            // 2) (Optional) If header missing, derive from subdomain (e.g. foo.example.com -> foo)
            // String host = request.getServerName();
            // String subdomain = deriveSubdomain(host); // implement if you want
            // if (orgHeader == null && subdomain != null) {
            //     organizations.findBySubdomainIgnoreCase(subdomain)
            //         .ifPresent(o -> orgHeader = o.getOrgId().toString());
            // }

            if (orgHeader != null && !orgHeader.isBlank()) {
                try {
                    UUID orgId = UUID.fromString(orgHeader.trim());
                    // Optional: ensure org exists & active up front
                    boolean exists = organizations.existsById(orgId);
                    if (exists) {
                        TenantContext.set(orgId);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID -> keep unset; your service will throw a clear error later
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}