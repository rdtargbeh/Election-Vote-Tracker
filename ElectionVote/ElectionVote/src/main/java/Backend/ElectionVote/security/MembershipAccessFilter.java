//package Backend.ElectionVote.security;
//
//import Backend.ElectionVote.repository.OrgMembershipRepository;
//import Backend.ElectionVote.utility.TenantContext;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.UUID;
//
///**
// * Enforces tenant membership enabled rule:
// *
// * ✅ Applies ONLY to tenant-scoped API endpoints.
// * ✅ SYSTEM_ADMIN bypasses membership checks and does NOT require orgId.
// * ✅ Blocks when org_membership.is_enabled = false (or membership missing).
// *
// * Robustness:
// * - Uses TenantContext first (set by TenantFilter).
// * - Falls back to JWT claims if TenantContext is missing userId/isSystemAdmin.
// */
//@Component
//@RequiredArgsConstructor
//public class MembershipAccessFilter extends OncePerRequestFilter {
//
//    private static final Logger log = LoggerFactory.getLogger(MembershipAccessFilter.class);
//
//    private final OrgMembershipRepository membershipRepository;
//
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        // Allow browser preflight
//        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
//
//        String p = request.getRequestURI();
//        // Never enforce membership on auth/public/health/static routes
//        return p.startsWith("/api/public/")
//                || p.startsWith("/api/auth/")
//                || p.startsWith("/actuator")
//                || p.startsWith("/favicon")
//                || p.startsWith("/assets")
//                || p.startsWith("/static");
//    }
//
//    /**
//     * Only enforce membership on tenant-scoped routes.
//     * Keep aligned with your tenant boundary.
//     *
//     * IMPORTANT:
//     * - /api/orgs is typically platform/admin scope (SYSTEM_ADMIN) AND tenant scope (tenant admins).
//     *   So we enforce membership ONLY if an orgId context exists OR the caller is not system-admin.
//     */
//    private boolean isTenantScoped(String path) {
//        if (path == null) return false;
//
//        return path.startsWith("/api/users/")
//                || path.startsWith("/api/votes/")
//                || path.startsWith("/api/chat/")
//                || path.startsWith("/api/tenants/");
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest req,
//                                    HttpServletResponse res,
//                                    FilterChain chain) throws ServletException, IOException {
//
//        final String path = req.getRequestURI();
//
//        // Not tenant scoped => do nothing
//        if (!isTenantScoped(path)) {
//            chain.doFilter(req, res);
//            return;
//        }
//
//        // -------------------------------
//        // 1) Read from TenantContext first
//        // -------------------------------
//        UUID orgId = TenantContext.getCurrentOrgIdOrNull();
//        UUID userId = TenantContext.getCurrentUserIdOrNull();
//        boolean isSystemAdmin = TenantContext.isSystemAdminContext();
//
//        // -------------------------------
//        // 2) Fallback to JWT claims if needed
//        // -------------------------------
//        if (userId == null || !isSystemAdmin) {
//            Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
//
//            Jwt jwt = null;
//            if (auth instanceof JwtAuthenticationToken jat) {
//                jwt = jat.getToken();
//            } else if (auth != null && auth.getPrincipal() instanceof Jwt j) {
//                jwt = j;
//            }
//
//            if (jwt != null) {
//                // userId claim fallback
//                if (userId == null) {
//                    userId = extractUuid(jwt, "userId", "user_id", "uid", "sub");
//                }
//
//                // isSystemAdmin claim fallback (authoritative for bypass)
//                Object v = jwt.getClaims().get("isSystemAdmin");
//                if (v instanceof Boolean b) isSystemAdmin = b;
//                else if (v instanceof String s) isSystemAdmin = Boolean.parseBoolean(s);
//            }
//        }
//
//        // ✅ SYSTEM_ADMIN bypass: no org required, no membership check
//        if (isSystemAdmin) {
//            chain.doFilter(req, res);
//            return;
//        }
//
//        // For non-system-admin, we require both org + user context
//        if (orgId == null) {
//            deny(res, "Organization context is missing. Please select an organization.");
//            return;
//        }
//        if (userId == null) {
//            deny(res, "User context is missing. Please log in again.");
//            return;
//        }
//
//        // ✅ Allow only if membership exists AND is enabled
//        boolean enabled = membershipRepository
//                .existsByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(orgId, userId);
//
//        if (!enabled) {
//            log.warn("MembershipAccessFilter denied: userId={}, orgId={}, path={}", userId, orgId, path);
//            deny(res, "Your membership in this organization is disabled. Please contact your administrator.");
//            return;
//        }
//
//        chain.doFilter(req, res);
//    }
//
//    private static UUID extractUuid(Jwt jwt, String... keys) {
//        for (String k : keys) {
//            Object v = jwt.getClaims().get(k);
//            if (v instanceof String s) {
//                try { return UUID.fromString(s); } catch (Exception ignored) {}
//            }
//        }
//        // also try subject
//        String sub = jwt.getSubject();
//        if (sub != null) {
//            try { return UUID.fromString(sub); } catch (Exception ignored) {}
//        }
//        return null;
//    }
//
//    private void deny(HttpServletResponse res, String message) throws IOException {
//        res.setStatus(HttpStatus.FORBIDDEN.value());
//        res.setContentType("application/json");
//        res.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"" + escapeJson(message) + "\"}");
//    }
//
//    private static String escapeJson(String s) {
//        if (s == null) return "";
//        return s.replace("\\", "\\\\").replace("\"", "\\\"");
//    }
//}
//
//
//
//
//
////
////package Backend.ElectionVote.security;
////
////import Backend.ElectionVote.entity.SystemUser;
////import Backend.ElectionVote.repository.OrgMembershipRepository;
////import Backend.ElectionVote.repository.SystemUserRepository;
////import Backend.ElectionVote.utility.TenantContext;
////import jakarta.servlet.FilterChain;
////import jakarta.servlet.ServletException;
////import jakarta.servlet.http.HttpServletRequest;
////import jakarta.servlet.http.HttpServletResponse;
////import lombok.RequiredArgsConstructor;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.http.HttpStatus;
////import org.springframework.security.authentication.AnonymousAuthenticationToken;
////import org.springframework.security.core.Authentication;
////import org.springframework.security.core.GrantedAuthority;
////import org.springframework.security.core.context.SecurityContextHolder;
////import org.springframework.security.oauth2.jwt.Jwt;
////import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
////import org.springframework.stereotype.Component;
////import org.springframework.web.filter.OncePerRequestFilter;
////
////import java.io.IOException;
////import java.util.UUID;
////
/////**
//// * MembershipAccessFilter
//// *
//// * Enforces: tenant users can access tenant-scoped endpoints only if membership is enabled.
//// *
//// * SYSTEM_ADMIN behavior:
//// * - SYSTEM_ADMIN may access platform/global routes WITHOUT orgId.
//// * - If orgId is present, SYSTEM_ADMIN still bypasses membership checks.
//// */
////@Component
////@RequiredArgsConstructor
////public class MembershipAccessFilter extends OncePerRequestFilter {
////
////    private static final Logger log = LoggerFactory.getLogger(MembershipAccessFilter.class);
////
////    private final OrgMembershipRepository membershipRepository;
////    private final SystemUserRepository systemUserRepository;
////
////    @Override
////    protected boolean shouldNotFilter(HttpServletRequest request) {
////        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
////
////        String p = request.getRequestURI();
////        return p.startsWith("/api/public/")
////                || p.startsWith("/api/auth/")
////                || p.startsWith("/actuator")
////                || p.startsWith("/favicon")
////                || p.startsWith("/assets")
////                || p.startsWith("/static");
////    }
////
////    /**
////     * Tenant-scoped endpoints where membership must be enabled.
////     * Keep aligned with your TenantFilter tenant-scoped logic.
////     *
////     * NOTE:
////     * - /api/users/me is used by both tenant users and SYSTEM_ADMIN.
////     * - We'll allow SYSTEM_ADMIN through even if orgId is missing.
////     */
////    private boolean isTenantScoped(String path) {
////        if (path == null) return false;
////
////        return path.startsWith("/api/users/")
////                || path.startsWith("/api/votes/")
////                || path.startsWith("/api/chat/")
////                || path.startsWith("/api/tenants/");
////    }
////
////    @Override
////    protected void doFilterInternal(HttpServletRequest req,
////                                    HttpServletResponse res,
////                                    FilterChain chain) throws ServletException, IOException {
////
////        final String path = req.getRequestURI();
////
////        // Not tenant scoped? do nothing
////        if (!isTenantScoped(path)) {
////            chain.doFilter(req, res);
////            return;
////        }
////
////        // ✅ Determine system admin FIRST (important)
////        boolean isSystemAdmin = isSystemAdmin();
////
////        // TenantFilter sets orgId when present & active.
////        UUID orgId = TenantContext.getCurrentOrgIdOrNull();
////
////        /**
////         * ✅ SYSTEM_ADMIN rule:
////         * - If orgId is missing, we allow the request through.
////         *   This supports global system admin endpoints like /api/users/me.
////         * - If orgId exists, still bypass membership checks.
////         */
////        if (isSystemAdmin) {
////            chain.doFilter(req, res);
////            return;
////        }
////
////        /**
////         * For non-system-admin tenant users:
////         * - orgId is REQUIRED for tenant-scoped routes.
////         */
////        if (orgId == null) {
////            deny(res, "Organization context is missing. Please select an organization.");
////            return;
////        }
////
////        // Resolve userId reliably
////        UUID userId = resolveUserId();
////        if (userId == null) {
////            deny(res, "User context is missing. Please log in again.");
////            return;
////        }
////
////        // Enforce membership enabled
////        boolean enabled = membershipRepository
////                .existsByOrganization_OrgIdAndUser_UserIdAndIsEnabledTrue(orgId, userId);
////
////        if (!enabled) {
////            log.warn("MembershipAccessFilter denied: userId={}, orgId={}, path={}", userId, orgId, path);
////            deny(res, "Your membership in this organization is disabled. Please contact your administrator.");
////            return;
////        }
////
////        chain.doFilter(req, res);
////    }
////
////    private boolean isSystemAdmin() {
////        if (TenantContext.isSystemAdminContext()) return true;
////
////        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
////        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return false;
////
////        return auth.getAuthorities().stream()
////                .map(GrantedAuthority::getAuthority)
////                .anyMatch(a -> a != null && (a.equals("ROLE_SYSTEM_ADMIN") || a.equals("SYSTEM_ADMIN") || a.endsWith("SYSTEM_ADMIN")));
////    }
////
////    private UUID resolveUserId() {
////        UUID fromCtx = TenantContext.getCurrentUserIdOrNull();
////        if (fromCtx != null) return fromCtx;
////
////        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
////        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
////
////        // JWT claims
////        if (auth instanceof JwtAuthenticationToken jwtAuth) {
////            Jwt jwt = jwtAuth.getToken();
////            UUID fromJwt = extractUuidClaim(jwt, "userId", "user_id", "uid", "sub");
////            if (fromJwt != null) return fromJwt;
////        } else if (auth.getPrincipal() instanceof Jwt jwt) {
////            UUID fromJwt = extractUuidClaim(jwt, "userId", "user_id", "uid", "sub");
////            if (fromJwt != null) return fromJwt;
////        }
////
////        // fallback to DB lookup by username/email
////        String identifier = auth.getName();
////        if (identifier == null || identifier.isBlank()) return null;
////
////        return systemUserRepository.findByUserNameIgnoreCase(identifier)
////                .or(() -> systemUserRepository.findByEmailIgnoreCase(identifier))
////                .map(SystemUser::getUserId)
////                .orElse(null);
////    }
////
////    private static UUID extractUuidClaim(Jwt jwt, String... names) {
////        if (jwt == null) return null;
////        for (String n : names) {
////            Object v = jwt.getClaims().get(n);
////            if (v instanceof String s) {
////                try { return UUID.fromString(s); } catch (Exception ignored) {}
////            }
////        }
////        String sub = jwt.getSubject();
////        if (sub != null) {
////            try { return UUID.fromString(sub); } catch (Exception ignored) {}
////        }
////        return null;
////    }
////
////    private void deny(HttpServletResponse res, String message) throws IOException {
////        res.setStatus(HttpStatus.FORBIDDEN.value());
////        res.setContentType("application/json");
////        res.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"" + escapeJson(message) + "\"}");
////    }
////
////    private static String escapeJson(String s) {
////        if (s == null) return "";
////        return s.replace("\\", "\\\\").replace("\"", "\\\"");
////    }
////}
////
