package Backend.ElectionVote.config;

import Backend.ElectionVote.security.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityCryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 10–12 is a good balance in dev; you can raise cost in prod.
        return new BCryptPasswordEncoder(12);
    }

}