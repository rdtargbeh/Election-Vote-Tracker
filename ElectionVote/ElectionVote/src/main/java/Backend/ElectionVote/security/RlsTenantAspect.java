package Backend.ElectionVote.security;

import Backend.ElectionVote.utility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@RequiredArgsConstructor
public class RlsTenantAspect {

    private final JdbcTemplate jdbc;

    /** Runs before any @Transactional public method in your app packages. Adjust package pointcut if needed. */
    @Before("execution(public * Backend.ElectionVote..*(..)) && @annotation(transactional)")
    public void setRlsVariables(Transactional transactional) {
        TenantContext ctx = TenantContext.get();
        if (ctx == null) return;

        String isAdmin = ctx.isSystemAdmin() ? "true" : "false";
        String org     = ctx.orgId().map(Object::toString).orElse("");

        // SET LOCAL is scoped to the current transaction/connection
        jdbc.execute("SET LOCAL app.is_system_admin = '" + isAdmin + "'");
        jdbc.execute("SET LOCAL app.current_org     = '" + org + "'");
    }
}