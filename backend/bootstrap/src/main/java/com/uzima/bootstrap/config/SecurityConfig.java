package com.uzima.bootstrap.config;

import com.uzima.bootstrap.adapter.http.security.JwtAuthenticationFilter;
import com.uzima.security.token.port.AccessTokenVerifierPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration Spring Security : Stateless JWT.
 *
 * Politique :
 * - STATELESS : pas de session HTTP (chaque requête est auto-portante via JWT)
 * - CSRF désactivé (inutile pour les APIs stateless avec tokens)
 * - CORS configuré explicitement (liste blanche d'origines)
 * - Security headers OWASP : X-Content-Type-Options, X-Frame-Options, CSP, Referrer-Policy
 * - Endpoints publics : /api/auth/login, /api/auth/register, /api/auth/token/refresh, /swagger-ui, /v3/api-docs
 * - Tous les autres endpoints requièrent un access token valide
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AccessTokenVerifierPort tokenVerifier,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenVerifier);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Security headers OWASP (A05:2021 Security Misconfiguration)
            .headers(headers -> headers
                .contentTypeOptions(c -> {})                          // X-Content-Type-Options: nosniff
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)                          // X-Frame-Options: DENY (anti-clickjacking)
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'none'; frame-ancestors 'none'")
                )
                .referrerPolicy(r -> r
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31_536_000)
                    .includeSubDomains(true)
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/token/refresh",
                    // OpenAPI / Swagger UI
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS explicite (OWASP A01:2021 — Broken Access Control).
     * <p>
     * En production, remplacer les origines par la liste blanche réelle.
     * Utiliser la variable d'environnement CORS_ALLOWED_ORIGINS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",     // dev mobile / web
                "https://*.uzima.app"     // production
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "X-User-Id"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false); // stateless JWT — pas de cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
