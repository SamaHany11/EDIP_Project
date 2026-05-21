package com.example.EDIP.Auth.security;

import com.example.EDIP.Auth.model.UserSession;
import com.example.EDIP.Auth.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.Auth.model.User;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JWTFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final UserSessionRepository userSessionRepository;
    private final TokenHashUtil tokenHashUtil;

    public JWTFilter(UserRepository userRepository, JWTUtil jwtUtil,
                     UserSessionRepository userSessionRepository,
                     TokenHashUtil tokenHashUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userSessionRepository = userSessionRepository;
        this.tokenHashUtil = tokenHashUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isRefreshEndpoint = path.contains("/refresh-token");

        final String authHeader = request.getHeader("Authorization");

        String email = null;
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);

            try {
                email = jwtUtil.extractEmail(token);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                if (isRefreshEndpoint) {
                    response.getWriter().write("invalid refresh token");
                }
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (jwtUtil.validateToken(token, "LOGIN")) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null || !user.isEnabled() || !Boolean.TRUE.equals(user.getIsActive())) {
                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Account is disabled or inactive"
                    );
                    return;
                }
                String hashedToken = tokenHashUtil.hashToken(token);
                Optional<UserSession> sessionOpt =
                        userSessionRepository.findByTokenHash(hashedToken);

                if (sessionOpt.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                UserSession session = sessionOpt.get();

                if (!session.getIsActive()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                if (session.getLastActivityTime()
                        .plusMinutes(480)
                        .isBefore(LocalDateTime.now())) {

                    session.setIsActive(false);
                    session.setLogoutTime(LocalDateTime.now());
                    userSessionRepository.save(session);

                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                session.setLastActivityTime(LocalDateTime.now());
                userSessionRepository.save(session);

                String role = jwtUtil.extractRole(token);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}