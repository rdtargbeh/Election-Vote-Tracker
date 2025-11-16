package Backend.ElectionVote.config;


import Backend.ElectionVote.security.RequestContextMdcFilter;
import Backend.ElectionVote.security.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Constructor-injected collaborators (safe: no PasswordEncoder here)
    private final TenantFilter tenantFilter;
    private final RequestContextMdcFilter requestContextMdcFilter;

    // ---------------------------------------------------------
    //  Password encoder
    // ---------------------------------------------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with log rounds = 12
        return new BCryptPasswordEncoder(12);
    }

    // ---------------------------------------------------------
    //  DaoAuthenticationProvider (username/password login)
    // ---------------------------------------------------------
    @Bean
    public AuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    // ---------------------------------------------------------
    //  Canonical AuthenticationManager used by /api/auth/login
    // ---------------------------------------------------------
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider daoAuthenticationProvider) {
        // You can add more providers to this list later if needed
        return new ProviderManager(daoAuthenticationProvider);
    }

    // ---------------------------------------------------------
    //  API security (JWT, stateless)
    // ---------------------------------------------------------
    @Bean
    @Order(0)
    SecurityFilterChain api(HttpSecurity http,
                            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/public/**",
                                "/api/auth/**",      // login/register/etc
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; form-action 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true))
                );

        // Tenant filter after JWT has established Authentication
        http.addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class);

        // MDC logging after tenant is bound
        http.addFilterAfter(requestContextMdcFilter, TenantFilter.class);

        return http.build();
    }

    // ---------------------------------------------------------
    //  UI / console security (form login, stateful)
    // ---------------------------------------------------------
    @Bean
    @Order(1)
    SecurityFilterChain ui(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/console/**", "/login", "/logout")
                .csrf(csrf -> csrf.ignoringRequestMatchers("/console/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        // Tenant filter after username/password auth
        http.addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        // MDC logging after tenant is bound
        http.addFilterAfter(requestContextMdcFilter, TenantFilter.class);

        return http.build();
    }

    // ---------------------------------------------------------
    //  CORS for SPA / frontend
    // ---------------------------------------------------------
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:3000"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Org-Id"));
        c.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

}