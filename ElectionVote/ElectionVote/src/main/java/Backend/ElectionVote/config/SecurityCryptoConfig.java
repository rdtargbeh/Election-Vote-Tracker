package Backend.ElectionVote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Central password encoder configuration.
 *
 * - Uses BCrypt, the modern standard for password hashing.
 * - Strength 12 is safe for production; increase if CPU allows.
 * - Automatically integrates with any AuthenticationManagerBuilder or UserDetailsService.
 */
@Configuration
public class SecurityCryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength (log rounds) 12. Increase to 13–14 on strong servers.
        return new BCryptPasswordEncoder(12);
    }
}