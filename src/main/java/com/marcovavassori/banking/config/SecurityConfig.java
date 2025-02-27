package com.marcovavassori.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import com.marcovavassori.banking.filters.JwtAuthenticationFilter;
import com.marcovavassori.banking.services.UserService;

/**
 * Central security configuration class for the application.
 * 
 * @Configuration: Indicates this is a Spring configuration class
 * @EnableWebSecurity: Enables Spring Security's web security support
 **/
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final UserService userService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;

        public SecurityConfig(UserService userService, JwtAuthenticationFilter jwtAuthenticationFilter,
                        CustomAccessDeniedHandler customAccessDeniedHandler) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.userService = userService;
                this.customAccessDeniedHandler = customAccessDeniedHandler;
        }

        /**
         * Defines the password encoding algorithm for the application.
         * BCrypt is a strong one-way hashing algorithm designed for passwords.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * Configures the security filter chain which defines the security rules
         * for different HTTP requests in the application.
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                // Disable CSRF (Cross Site Request Forgery) protection
                                // Safe for REST APIs that use JWT tokens
                                .csrf(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers
                                                .contentSecurityPolicy(
                                                                csp -> csp.policyDirectives("default-src 'self';"))
                                                .frameOptions(frameOptions -> frameOptions.deny())
                                                .xssProtection(xss -> xss.headerValue(
                                                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .cacheControl(cache -> cache.disable()))
                                // Add sessionManagement configuration
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Configure authorization rules for HTTP requests
                                .authorizeHttpRequests(
                                                req -> req
                                                                // Public endpoints that don't require authentication
                                                                .requestMatchers(
                                                                                "/", // Home page
                                                                                "/api/auth/signin/**",
                                                                                "/api/auth/signup/**",
                                                                                "/api/auth/refresh-token",
                                                                                "/api/auth/signout",
                                                                                "/error"
                                                                // endpoint
                                                                ).permitAll()
                                                                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                                                                // All other endpoints require authentication
                                                                .anyRequest().authenticated())
                                // Set the service to load user details when authenticating
                                .userDetailsService(userService)
                                .exceptionHandling(e -> e.accessDeniedHandler(customAccessDeniedHandler)
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                // Add our custom JWT filter before the standard authentication filter
                                // This ensures JWT authentication happens before username/password
                                // authentication
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        /**
         * Creates the authentication manager bean.
         * The authentication manager is responsible for processing authentication
         * requests.
         * It uses the configured UserDetailsService and PasswordEncoder to authenticate
         * users.
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }
}