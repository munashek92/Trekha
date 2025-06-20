package com.app.trekha.config.security;

import com.app.trekha.user.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
        final String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", authHeader);

        final String jwt;
        final String userEmailOrMobile; // This will be the username (email or mobile)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No JWT token found in Authorization header or does not start with Bearer. Passing to next filter.");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        logger.debug("Extracted JWT: {}", jwt);
        userEmailOrMobile = jwtService.extractUsername(jwt);

        if (userEmailOrMobile != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmailOrMobile);
            logger.debug("UserDetails loaded: {}", userDetails != null ? userDetails.getUsername() : "null"); // LOG THIS

            if (jwtService.isTokenValid(jwt, userDetails)) {
                logger.debug("JWT is valid.");

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Credentials are not needed here as JWT is already validated
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Authentication set in SecurityContextHolder for user: {}", userEmailOrMobile); // LOG THIS

            } else{
                logger.warn("JWT validation failed for token: {}", jwt); // LOG THIS

            }
        }
        filterChain.doFilter(request, response);
    }
}
