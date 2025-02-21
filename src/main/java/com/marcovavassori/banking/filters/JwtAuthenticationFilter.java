package com.marcovavassori.banking.filters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.marcovavassori.banking.services.JwtService;
import com.marcovavassori.banking.services.UserService;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract the JWT token from the Authorization header
        String authHeader = request.getHeader("Authorization");

        // If no token is present or it's not a Bearer token, continue the filter chain
        // without authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Step 2: Clean the token by removing the "Bearer " prefix
        String token = authHeader.substring(7);

        // Step 3: Extract the username from the token
        String username = jwtService.extractUsernameFromToken(token);

        // Step 4: Authenticate the user if:
        // - Username was successfully extracted from token
        // - No authentication exists in the current security context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Step 4.1: Load user details from the database
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Step 4.2: Validate the token against the user details
            if (jwtService.validateToken(token, userDetails)) {
                // Step 4.3: Create authentication token with user's authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, // credentials (not needed after authentication)
                        userDetails.getAuthorities());

                // Step 4.4: Add request details to authentication token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Step 4.5: Set the authentication in the Security Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 5: Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
