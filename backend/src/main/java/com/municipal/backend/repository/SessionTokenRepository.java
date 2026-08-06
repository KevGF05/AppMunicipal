package com.municipal.backend.repository;

import com.municipal.backend.model.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessionTokenRepository extends JpaRepository<SessionToken, String> {
    Optional<SessionToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);
    void deleteByUserId(Long userId);
}
