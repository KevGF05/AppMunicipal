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
import java.util.UUID;

@Service
public class ReportService {
    private final ReportRepository reports;
    private final AppUserRepository users;
    private final ReportHistoryRepository history;
    private final StorageService storage;

    public ReportService(
        ReportRepository reports,
        AppUserRepository users,
        ReportHistoryRepository history,
        StorageService storage
    ) {
        this.reports = reports;
        this.users = users;
        this.history = history;
        this.storage = storage;
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
        report.setCode("EP-" + Year.now().getValue() + "-" + String.format("%06d", report.getId()));
        report = reports.save(report);
        addHistory(report, null, ReportStatus.CREADO, "Ciudadano anónimo", "Reporte creado desde la aplicación.");
        return new CreateReportResponse(report.getId(), report.getCode(), report.getTrackingToken(), report.getStatus().name(), "Reporte registrado correctamente.");
    }

    @Transactional(readOnly = true)
    public TrackingResponse track(String code, String token) {
        Report report = reports.findByCodeAndTrackingToken(code.trim(), token.trim())
            .orElseThrow(() -> new NotFoundException("No se encontró un reporte con ese código y token."));
        return new TrackingResponse(
            report.getCode(), report.getType(), report.getDescription(), report.getAddress(),
            report.getStatus().name(), report.getPriority().name(), report.getCreatedAt(), report.getUpdatedAt(),
            report.getRejectionReason(), report.getClosureComment()
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
        if (report.getStatus() != ReportStatus.APROBADO && report.getStatus() != ReportStatus.ASIGNADO) {
            throw new BusinessException("El reporte debe estar aprobado antes de asignarlo.");
        }
        AppUser technician = users.findById(request.technicianId())
            .filter(user -> user.isActive() && user.getRole() == Role.TECNICO)
            .orElseThrow(() -> new NotFoundException("Técnico no encontrado."));
        ReportStatus previous = report.getStatus();
        report.setAssignedTechnician(technician);
        report.setPriority(request.priority());
        report.setStatus(ReportStatus.ASIGNADO);
        reports.save(report);
        addHistory(report, previous, ReportStatus.ASIGNADO, admin.getName(),
            "Asignado a " + technician.getName() + optional(". ", request.observation()));
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public List<TechnicianResponse> technicians() {
        return users.findByRoleAndActiveTrueOrderByNameAsc(Role.TECNICO).stream()
            .map(user -> new TechnicianResponse(user.getId(), user.getName(), user.getEmail()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listTechnician(AppUser technician) {
        return reports.findByAssignedTechnicianIdOrderByCreatedAtDesc(technician.getId()).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getTechnician(Long id, AppUser technician) {
        Report report = find(id);
        requireAssigned(report, technician);
        return toResponse(report);
    }

    @Transactional
    public ReportResponse start(Long id, AppUser technician) {
        Report report = find(id);
        requireAssigned(report, technician);
        requireStatus(report, ReportStatus.ASIGNADO);
        transition(report, ReportStatus.EN_PROCESO, technician.getName(), "Trabajo iniciado.");
        return toResponse(report);
    }

    @Transactional
    public ReportResponse close(Long id, String comment, MultipartFile finalPhoto, AppUser technician) {
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

    private void validatePublic(String type, String description, String address, double latitude, double longitude) {
        if (type == null || type.isBlank() || type.length() > 100) throw new BusinessException("Tipo de reporte inválido.");
        if (description == null || description.trim().length() < 10 || description.length() > 1000) throw new BusinessException("La descripción debe contener entre 10 y 1000 caracteres.");
        if (address == null || address.isBlank() || address.length() > 300) throw new BusinessException("Ubicación inválida.");
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException("Coordenadas inválidas.");
        }
    }

    private Report find(Long id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("Reporte no encontrado."));
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
            report.getLatitude(), report.getLongitude(), report.getInitialPhotoPath(), report.getFinalPhotoPath(),
            report.getStatus().name(), report.getPriority().name(), technician == null ? null : technician.getId(),
            technician == null ? null : technician.getName(), report.getClosureComment(), report.getRejectionReason(),
            report.getCreatedAt(), report.getUpdatedAt(), items
        );
    }

    private String optional(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value.trim();
    }
}
