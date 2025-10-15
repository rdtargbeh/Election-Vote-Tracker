package Backend.ElectionVote.security;

import Backend.ElectionVote.uility.TenantContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class RlsTenantAspect {
    private final JdbcTemplate jdbc;

    /** Runs before any @Transactional public method in your app packages. */
    @Before("execution(public * Backend.ElectionVote..*(..)) && @annotation(transactional)")
    public void setRlsVariable(Transactional transactional) {
        UUID orgId = TenantContext.get();
        if (orgId != null) {
            // SET LOCAL lives only for the current transaction/connection
            jdbc.execute("SET LOCAL app.current_org = '" + orgId + "'");
        }
    }
}