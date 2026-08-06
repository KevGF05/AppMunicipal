package com.municipal.backend.security;

import com.municipal.backend.model.AppUser;
import com.municipal.backend.repository.SessionTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    private final SessionTokenRepository sessionTokenRepository;

    public BearerTokenFilter(SessionTokenRepository sessionTokenRepository) {
        this.sessionTokenRepository = sessionTokenRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            sessionTokenRepository.findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .ifPresent(session -> authenticate(session.getUser(), token));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(AppUser user, String token) {
        if (SecurityContextHolder.getContext().getAuthentication() != null || !user.isActive()) return;
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        var authentication = new UsernamePasswordAuthenticationToken(user, token, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
