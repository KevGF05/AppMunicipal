package com.municipal.backend.service;

import com.municipal.backend.dto.NotificationResponse;
import com.municipal.backend.exception.NotFoundException;
import com.municipal.backend.model.AppNotification;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.Report;
import com.municipal.backend.model.Role;
import com.municipal.backend.repository.AppNotificationRepository;
import com.municipal.backend.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final AppNotificationRepository notifications;
    private final AppUserRepository users;

    public NotificationService(AppNotificationRepository notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void notifyAdminsNewReport(Report report) {
        for (AppUser admin : users.findByRoleAndActiveTrueOrderByNameAsc(Role.ADMINISTRADOR)) {
            create(
                admin,
                "Nuevo reporte municipal",
                "Se recibió el ticket " + report.getCode() + ": " + report.getType() + ".",
                report.getId()
            );
        }
    }

    @Transactional
    public void notifyTechnicianAssigned(Report report, AppUser technician) {
        create(
            technician,
            "Nuevo ticket asignado",
            "Se le asignó el ticket " + report.getCode() + ": " + report.getType() + ".",
            report.getId()
        );
    }

    @Transactional
    public void notifyTechnicianReassigned(
        Report report,
        AppUser previousTechnician,
        AppUser newTechnician
    ) {
        create(
            previousTechnician,
            "Ticket reasignado",
            "El ticket " + report.getCode() + " fue reasignado a otro técnico.",
            report.getId()
        );
        create(
            newTechnician,
            "Ticket reasignado a su cuenta",
            "Se le reasignó el ticket " + report.getCode() + ": " + report.getType() + ".",
            report.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(AppUser user, boolean unreadOnly) {
        List<AppNotification> items = unreadOnly
            ? notifications.findByRecipientIdAndReadFalseOrderByCreatedAtAsc(user.getId())
            : notifications.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id, AppUser user) {
        AppNotification item = notifications.findById(id)
            .filter(value -> value.getRecipient().getId().equals(user.getId()))
            .orElseThrow(() -> new NotFoundException("Notificación no encontrada."));
        item.setRead(true);
        return toResponse(notifications.save(item));
    }

    private void create(AppUser recipient, String title, String message, Long reportId) {
        AppNotification item = new AppNotification();
        item.setRecipient(recipient);
        item.setTitle(title);
        item.setMessage(message);
        item.setReportId(reportId);
        item.setRead(false);
        notifications.save(item);
    }

    private NotificationResponse toResponse(AppNotification item) {
        return new NotificationResponse(
            item.getId(),
            item.getTitle(),
            item.getMessage(),
            item.getReportId(),
            item.isRead(),
            item.getCreatedAt()
        );
    }
}
