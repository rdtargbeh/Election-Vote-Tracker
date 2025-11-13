package Backend.ElectionVote.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // TODO: Replace with your auth context (e.g., Spring Security principal or tenant user)
        return () -> Optional.ofNullable(CurrentUserHolder.getUsername()).or(() -> Optional.of("system"));
    }


    // Minimal stub so code compiles if you don’t have security yet:
    static final class CurrentUserHolder {
        static String getUsername() {
            // integrate with SecurityContextHolder.getContext().getAuthentication().getName()
            return null;
        }
    }
}