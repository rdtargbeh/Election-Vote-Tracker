package Backend.ElectionVote.config;

import Backend.ElectionVote.security.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityProvidersConfig {

    @Bean
    public CurrentUserProvider currentUserProvider() {
        // uses your static factory that reads SecurityContextHolder
        return CurrentUserProvider.springSecurity();
    }
}