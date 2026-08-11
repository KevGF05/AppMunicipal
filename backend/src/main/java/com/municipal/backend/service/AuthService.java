package com.municipal.backend.service;

import com.municipal.backend.dto.LoginRequest;
import com.municipal.backend.dto.LoginResponse;
import com.municipal.backend.dto.PasswordChangeRequest;
import com.municipal.backend.exception.BusinessException;
import com.municipal.backend.exception.NotFoundException;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.Role;
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

        if (user.getRole() == Role.TECNICO && user.getPasswordChangeRequired() == null) {
            user.setPasswordChangeRequired(true);
            user = users.save(user);
        }
        if (user.getRole() == Role.ADMINISTRADOR && user.getPasswordChangeRequired() == null) {
            user.setPasswordChangeRequired(false);
            user = users.save(user);
        }

        sessions.deleteByUserId(user.getId());
        SessionToken session = new SessionToken();
        session.setToken(UUID.randomUUID().toString());
        session.setUser(user);
        session.setExpiresAt(LocalDateTime.now().plusHours(8));
        sessions.save(session);
        return new LoginResponse(
            session.getToken(),
            user.getName(),
            user.getRole().name(),
            Boolean.TRUE.equals(user.getPasswordChangeRequired())
        );
    }

    @Transactional
    public void changePassword(AppUser authenticatedUser, PasswordChangeRequest request) {
        AppUser user = users.findById(authenticatedUser.getId())
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));

        String newPassword = request.newPassword();
        if (!newPassword.equals(request.confirmPassword())) {
            throw new BusinessException("Las contraseñas no coinciden.");
        }
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new BusinessException("La contraseña debe contener entre 8 y 100 caracteres.");
        }
        if (!newPassword.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*")) {
            throw new BusinessException("La contraseña debe incluir al menos una letra.");
        }
        if (!newPassword.matches(".*\\d.*")) {
            throw new BusinessException("La contraseña debe incluir al menos un número.");
        }
        if (encoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la contraseña actual.");
        }

        user.setPasswordHash(encoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        users.save(user);
    }
}
