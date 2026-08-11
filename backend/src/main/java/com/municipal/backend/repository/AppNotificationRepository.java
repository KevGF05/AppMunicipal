package com.municipal.backend.repository;

import com.municipal.backend.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<AppNotification> findByRecipientIdAndReadFalseOrderByCreatedAtAsc(Long recipientId);
}
