package com.glowslot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration.
 * 
 * - Disables CSRF (Cross-Site Request Forgery) protection: Standard for stateless REST APIs using JWT/Bearer/tokens.
 * - Disables Sessions (STATELESS): Every HTTP request is standalone and self-authenticated.
 * - Allows public customer routes: /api/v1/public/**
 * - Allows barber workstation routes: /api/v1/barber/**
 * - Allows H2 in-memory DB console: /h2-console/**
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {}) // Uses CorsConfig bean
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // Allows H2 console iframes
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public consumer booking endpoints
                .requestMatchers("/api/v1/public/**").permitAll()
                // Barber & operational workstation endpoints
                .requestMatchers("/api/v1/barber/**").permitAll()
                // H2 web console for development
                .requestMatchers("/h2-console/**").permitAll()
                // Any other health or root checks
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
