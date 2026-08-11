package com.municipal.backend.service;

import com.municipal.backend.dto.*;
import com.municipal.backend.exception.BusinessException;
import com.municipal.backend.exception.NotFoundException;
import com.municipal.backend.model.*;
import com.municipal.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportService {
    private static final Set<String> ALLOWED_PUBLIC_TYPES = Set.of(
        "Señal de tránsito caída",
        "Señal de tránsito dañada",
        "Semáforo dañado",
        "Automóvil mal estacionado"
    );

    private final ReportRepository reports;
    private final AppUserRepository users;
    private final ReportHistoryRepository history;
    private final StorageService storage;
    private final NotificationService notificationService;

    public ReportService(
        ReportRepository reports,
        AppUserRepository users,
        ReportHistoryRepository history,
        StorageService storage,
        NotificationService notificationService
    ) {
        this.reports = reports;
        this.users = users;
        this.history = history;
        this.storage = storage;
        this.notificationService = notificationService;
    }

    @Transactional
    public CreateReportResponse createAnonymous(
        String type,
        String description,
        String address,
        double latitude,
        double longitude,
        MultipartFile photo
    ) {
        validatePublic(type, description, address, latitude, longitude);
        Report report = new Report();
        report.setTrackingToken(UUID.randomUUID().toString());
        report.setType(type.trim());
        report.setDescription(description.trim());
        report.setAddress(address.trim());
        report.setLatitude(latitude);
        report.setLongitude(longitude);
        report.setInitialPhotoPath(storage.storeImage(photo, "inicial"));
        report.setStatus(ReportStatus.CREADO);
        report.setPriority(Priority.MEDIA);
        report = reports.save(report);
        report.setCode(generateUniquePublicCode());
        report = reports.save(report);
        addHistory(report, null, ReportStatus.CREADO, "Ciudadano anónimo", "Reporte creado desde la aplicación.");
        notificationService.notifyAdminsNewReport(report);
        return new CreateReportResponse(
            report.getId(),
            report.getCode(),
            report.getStatus().name(),
            "Reporte registrado correctamente."
        );
    }

    @Transactional(readOnly = true)
    public TrackingResponse track(String code) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (normalizedCode.isBlank()) {
            throw new BusinessException("Debe ingresar el código de seguimiento.");
        }
        Report report = reports.findByCode(normalizedCode)
            .orElseThrow(() -> new NotFoundException("No se encontró un reporte con ese código."));
        return new TrackingResponse(
            report.getCode(),
            report.getStatus().name(),
            report.getUpdatedAt(),
            publicStatusMessage(report.getStatus())
        );
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listAdmin(ReportStatus status) {
        List<Report> values = status == null
            ? reports.findAllByOrderByCreatedAtDesc()
            : reports.findByStatusOrderByCreatedAtDesc(status);
        return values.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getAdmin(Long id) { return toResponse(find(id)); }

    @Transactional
    public ReportResponse approve(Long id, AppUser admin) {
        Report report = find(id);
        requireStatus(report, ReportStatus.CREADO);
        transition(report, ReportStatus.APROBADO, admin.getName(), "Reporte aprobado.");
        return toResponse(report);
    }

    @Transactional
    public ReportResponse reject(Long id, String reason, AppUser admin) {
        Report report = find(id);
        if (report.getStatus() != ReportStatus.CREADO && report.getStatus() != ReportStatus.APROBADO) {
            throw new BusinessException("Solo se puede rechazar un reporte creado o aprobado.");
        }
        report.setRejectionReason(reason.trim());
        transition(report, ReportStatus.RECHAZADO, admin.getName(), reason.trim());
        return toResponse(report);
    }

    @Transactional
    public ReportResponse assign(Long id, AssignRequest request, AppUser admin) {
        Report report = find(id);
        if (report.getStatus() != ReportStatus.APROBADO) {
            throw new BusinessException("Solo se puede asignar un ticket aprobado que aún no tenga técnico.");
        }
        if (report.getAssignedTechnician() != null) {
            throw new BusinessException("Este ticket ya tiene un técnico. Use la opción Reasignar técnico.");
        }
        AppUser technician = activeTechnician(request.technicianId());
        ReportStatus previous = report.getStatus();
        report.setAssignedTechnician(technician);
        report.setPriority(request.priority());
        report.setStatus(ReportStatus.ASIGNADO);
        reports.save(report);
        addHistory(report, previous, ReportStatus.ASIGNADO, admin.getName(),
            "Asignado a " + technician.getName() + optional(". ", request.observation()));
        notificationService.notifyTechnicianAssigned(report, technician);
        return toResponse(report);
    }

    @Transactional
    public ReportResponse reassign(Long id, AssignRequest request, AppUser admin) {
        Report report = find(id);
        if (report.getStatus() != ReportStatus.ASIGNADO && report.getStatus() != ReportStatus.EN_PROCESO) {
            throw new BusinessException("Solo se puede reasignar un ticket asignado o en proceso.");
        }
        AppUser currentTechnician = report.getAssignedTechnician();
        if (currentTechnician == null) {
            throw new BusinessException("El ticket no tiene un técnico asignado. Use la opción Asignar técnico.");
        }
        AppUser newTechnician = activeTechnician(request.technicianId());
        if (currentTechnician.getId().equals(newTechnician.getId())) {
            throw new BusinessException("Seleccione un técnico diferente al que ya tiene asignado el ticket.");
        }

        ReportStatus previous = report.getStatus();
        report.setAssignedTechnician(newTechnician);
        report.setPriority(request.priority());
        report.setStatus(ReportStatus.ASIGNADO);
        reports.save(report);
        addHistory(
            report,
            previous,
            ReportStatus.ASIGNADO,
            admin.getName(),
            "Reasignado de " + currentTechnician.getName() + " a " + newTechnician.getName()
                + optional(". ", request.observation())
        );
        notificationService.notifyTechnicianReassigned(report, currentTechnician, newTechnician);
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public List<TechnicianResponse> technicians() {
        return users.findByRoleAndActiveTrueOrderByNameAsc(Role.TECNICO).stream()
            .map(user -> {
                String firstName = user.getFirstName();
                String lastName = user.getLastName();
                if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                    String[] pieces = user.getName().trim().split("\\s+");
                    firstName = pieces.length > 0 ? pieces[0] : "Técnico";
                    lastName = pieces.length > 1 ? pieces[pieces.length - 1] : "Municipal";
                }
                return new TechnicianResponse(
                    user.getId(), firstName, lastName, user.getName(), user.getEmail(), user.getPhoneNumber(),
                    user.isActive(), Boolean.TRUE.equals(user.getPasswordChangeRequired())
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listTechnician(AppUser technician) {
        requirePasswordChanged(technician);
        return reports.findByAssignedTechnicianIdOrderByCreatedAtDesc(technician.getId()).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getTechnician(Long id, AppUser technician) {
        requirePasswordChanged(technician);
        Report report = find(id);
        requireAssigned(report, technician);
        return toResponse(report);
    }

    @Transactional
    public ReportResponse start(Long id, AppUser technician) {
        requirePasswordChanged(technician);
        Report report = find(id);
        requireAssigned(report, technician);
        requireStatus(report, ReportStatus.ASIGNADO);
        transition(report, ReportStatus.EN_PROCESO, technician.getName(), "Trabajo iniciado.");
        return toResponse(report);
    }

    @Transactional
    public ReportResponse close(Long id, String comment, MultipartFile finalPhoto, AppUser technician) {
        requirePasswordChanged(technician);
        Report report = find(id);
        requireAssigned(report, technician);
        if (report.getStatus() != ReportStatus.ASIGNADO && report.getStatus() != ReportStatus.EN_PROCESO) {
            throw new BusinessException("El ticket no se encuentra disponible para cierre.");
        }
        if (comment == null || comment.trim().length() < 10 || comment.trim().length() > 1000) {
            throw new BusinessException("El comentario de cierre debe contener entre 10 y 1000 caracteres.");
        }
        report.setFinalPhotoPath(storage.storeImage(finalPhoto, "final"));
        report.setClosureComment(comment.trim());
        transition(report, ReportStatus.CERRADO, technician.getName(), comment.trim());
        return toResponse(report);
    }

    private String generateUniquePublicCode() {
        String code;
        do {
            String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
            code = "EP-" + Year.now().getValue() + "-" + randomPart;
        } while (reports.existsByCode(code));
        return code;
    }

    private String publicStatusMessage(ReportStatus status) {
        return switch (status) {
            case CREADO -> "El reporte fue recibido y está pendiente de revisión.";
            case APROBADO -> "El reporte fue aprobado por la municipalidad.";
            case RECHAZADO -> "El reporte fue revisado y rechazado.";
            case ASIGNADO -> "El reporte fue asignado al personal técnico.";
            case EN_PROCESO -> "El trabajo se encuentra en proceso.";
            case CERRADO -> "El reporte fue atendido y cerrado.";
        };
    }

    private void validatePublic(String type, String description, String address, double latitude, double longitude) {
        if (type == null || !ALLOWED_PUBLIC_TYPES.contains(type.trim())) {
            throw new BusinessException("Tipo de reporte inválido.");
        }
        if (description == null || description.trim().length() < 10 || description.length() > 1000) throw new BusinessException("La descripción debe contener entre 10 y 1000 caracteres.");
        if (address == null || address.isBlank() || address.length() > 300) throw new BusinessException("Ubicación inválida.");
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException("Coordenadas inválidas.");
        }
    }

    private AppUser activeTechnician(Long technicianId) {
        return users.findById(technicianId)
            .filter(user -> user.isActive() && user.getRole() == Role.TECNICO)
            .orElseThrow(() -> new NotFoundException("Técnico activo no encontrado."));
    }

    private Report find(Long id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("Reporte no encontrado."));
    }

    private void requirePasswordChanged(AppUser technician) {
        if (Boolean.TRUE.equals(technician.getPasswordChangeRequired())) {
            throw new BusinessException("Debe cambiar la contraseña temporal antes de gestionar tickets.");
        }
    }

    private void requireAssigned(Report report, AppUser technician) {
        if (report.getAssignedTechnician() == null || !report.getAssignedTechnician().getId().equals(technician.getId())) {
            throw new BusinessException("Este ticket no está asignado a su cuenta.");
        }
    }

    private void requireStatus(Report report, ReportStatus expected) {
        if (report.getStatus() != expected) {
            throw new BusinessException("Estado inválido. Se esperaba " + expected.name() + ".");
        }
    }

    private void transition(Report report, ReportStatus next, String actor, String comment) {
        ReportStatus previous = report.getStatus();
        report.setStatus(next);
        reports.save(report);
        addHistory(report, previous, next, actor, comment);
    }

    private void addHistory(Report report, ReportStatus previous, ReportStatus next, String actor, String comment) {
        ReportHistory item = new ReportHistory();
        item.setReport(report);
        item.setPreviousStatus(previous);
        item.setNewStatus(next);
        item.setActor(actor);
        item.setComment(comment);
        history.save(item);
    }

    private ReportResponse toResponse(Report report) {
        List<HistoryResponse> items = history.findByReportIdOrderByCreatedAtAsc(report.getId()).stream()
            .map(item -> new HistoryResponse(
                item.getPreviousStatus() == null ? null : item.getPreviousStatus().name(),
                item.getNewStatus().name(), item.getActor(), item.getComment(), item.getCreatedAt()
            )).toList();
        AppUser technician = report.getAssignedTechnician();
        return new ReportResponse(
            report.getId(), report.getCode(), report.getType(), report.getDescription(), report.getAddress(),
            report.getLatitude(), report.getLongitude(), storage.resolveUrl(report.getInitialPhotoPath()), storage.resolveUrl(report.getFinalPhotoPath()),
            report.getStatus().name(), report.getPriority().name(), technician == null ? null : technician.getId(),
            technician == null ? null : technician.getName(), report.getClosureComment(), report.getRejectionReason(),
            report.getCreatedAt(), report.getUpdatedAt(), items
        );
    }

    private String optional(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value.trim();
    }
}
