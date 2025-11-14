package Backend.ElectionVote.config;

import Backend.ElectionVote.security.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TenantFilter tenantFilter;


    /**
     * Stateless API secured by OAuth2 Resource Server (JWT).
     * CSRF disabled (JWT), strict headers, tenant filter runs after JWT auth.
     */
    @Bean
    @Order(0)
    SecurityFilterChain api(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                // Requires spring-boot-starter-oauth2-resource-server on classpath
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; form-action 'none'"))
                        // Do NOT use xssProtection(); it is removed & obsolete
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
                )
                // Ensure tenant resolution runs AFTER JWT auth (SecurityContext populated)
                .addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain ui(HttpSecurity http) throws Exception {
        http
                // Browser UI (if you have a console): form login with sessions
                .securityMatcher("/console/**", "/login", "/logout")
                .csrf(csrf -> csrf.ignoringRequestMatchers("/console/api/**")) // keep CSRF on for UI, ignore if needed
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                .headers(h -> h.frameOptions(f -> f.sameOrigin())); // if you need H2 console dev

        // TenantFilter for UI too (after auth established)
        http.addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }



}