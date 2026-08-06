package com.municipal.backend.service;

import com.municipal.backend.dto.LoginRequest;
import com.municipal.backend.dto.LoginResponse;
import com.municipal.backend.exception.BusinessException;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.SessionToken;
import com.municipal.backend.repository.AppUserRepository;
import com.municipal.backend.repository.SessionTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final SessionTokenRepository sessions;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository users, SessionTokenRepository sessions, PasswordEncoder encoder) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new BusinessException("Credenciales incorrectas."));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Credenciales incorrectas.");
        }
        sessions.deleteByUserId(user.getId());
        SessionToken session = new SessionToken();
        session.setToken(UUID.randomUUID().toString());
        session.setUser(user);
        session.setExpiresAt(LocalDateTime.now().plusHours(8));
        sessions.save(session);
        return new LoginResponse(session.getToken(), user.getName(), user.getRole().name());
    }
}
