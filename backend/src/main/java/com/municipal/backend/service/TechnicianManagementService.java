package com.municipal.backend.service;

import com.municipal.backend.dto.TechnicianCreateRequest;
import com.municipal.backend.dto.TechnicianResponse;
import com.municipal.backend.dto.TechnicianUpdateRequest;
import com.municipal.backend.exception.BusinessException;
import com.municipal.backend.exception.NotFoundException;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.ReportStatus;
import com.municipal.backend.model.Role;
import com.municipal.backend.repository.AppUserRepository;
import com.municipal.backend.repository.ReportRepository;
import com.municipal.backend.repository.SessionTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class TechnicianManagementService {
    private static final String TECHNICIAN_EMAIL_DOMAIN = "@pmunicipalidad.hn";

    private final AppUserRepository users;
    private final ReportRepository reports;
    private final SessionTokenRepository sessions;
    private final PasswordEncoder encoder;

    @Value("${APP_TECNICO1_PASSWORD:Tecnico123*}")
    private String defaultTechnicianPassword;

    public TechnicianManagementService(
        AppUserRepository users,
        ReportRepository reports,
        SessionTokenRepository sessions,
        PasswordEncoder encoder
    ) {
        this.users = users;
        this.reports = reports;
        this.sessions = sessions;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<TechnicianResponse> list(boolean onlyActive) {
        List<AppUser> technicians = onlyActive
            ? users.findByRoleAndActiveTrueOrderByNameAsc(Role.TECNICO)
            : users.findByRoleOrderByNameAsc(Role.TECNICO);
        return technicians.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TechnicianResponse create(TechnicianCreateRequest request) {
        String firstName = cleanName(request.firstName(), "nombre");
        String lastName = cleanName(request.lastName(), "apellido");
        String phone = cleanPhone(request.phoneNumber());

        AppUser technician = new AppUser();
        technician.setFirstName(firstName);
        technician.setLastName(lastName);
        technician.setName(fullName(firstName, lastName));
        technician.setEmail(generateUniqueEmail(firstName, lastName, null));
        technician.setPhoneNumber(phone);
        technician.setPasswordHash(encoder.encode(defaultTechnicianPassword));
        technician.setPasswordChangeRequired(true);
        technician.setRole(Role.TECNICO);
        technician.setActive(true);
        return toResponse(users.save(technician));
    }

    @Transactional
    public TechnicianResponse update(Long id, TechnicianUpdateRequest request) {
        AppUser technician = findTechnician(id);
        String firstName = cleanName(request.firstName(), "nombre");
        String lastName = cleanName(request.lastName(), "apellido");
        String phone = cleanPhone(request.phoneNumber());

        technician.setFirstName(firstName);
        technician.setLastName(lastName);
        technician.setName(fullName(firstName, lastName));
        technician.setEmail(generateUniqueEmail(firstName, lastName, id));
        technician.setPhoneNumber(phone);
        return toResponse(users.save(technician));
    }

    @Transactional
    public TechnicianResponse resetPassword(Long id) {
        AppUser technician = findTechnician(id);
        technician.setPasswordHash(encoder.encode(defaultTechnicianPassword));
        technician.setPasswordChangeRequired(true);
        AppUser saved = users.save(technician);
        sessions.deleteByUserId(id);
        return toResponse(saved);
    }

    @Transactional
    public TechnicianResponse setActive(Long id, boolean active) {
        AppUser technician = findTechnician(id);
        if (!active && reports.existsByAssignedTechnicianIdAndStatusIn(
            id,
            List.of(ReportStatus.ASIGNADO, ReportStatus.EN_PROCESO)
        )) {
            throw new BusinessException(
                "No se puede desactivar al técnico mientras tenga tickets asignados o en proceso. Reasigne esos tickets primero."
            );
        }

        technician.setActive(active);
        AppUser saved = users.save(technician);
        if (!active) sessions.deleteByUserId(id);
        return toResponse(saved);
    }

    private AppUser findTechnician(Long id) {
        return users.findById(id)
            .filter(user -> user.getRole() == Role.TECNICO)
            .orElseThrow(() -> new NotFoundException("Técnico no encontrado."));
    }

    public TechnicianResponse toResponse(AppUser user) {
        String[] parts = nameParts(user);
        return new TechnicianResponse(
            user.getId(),
            parts[0],
            parts[1],
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.isActive(),
            Boolean.TRUE.equals(user.getPasswordChangeRequired())
        );
    }

    private String[] nameParts(AppUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return new String[] { firstName, lastName };
        }

        String full = user.getName() == null ? "" : user.getName().trim();
        if (full.isBlank()) return new String[] { "Técnico", "Municipal" };
        String[] pieces = full.split("\\s+");
        if (pieces.length == 1) return new String[] { pieces[0], "Municipal" };
        return new String[] { pieces[0], pieces[pieces.length - 1] };
    }

    private String generateUniqueEmail(String firstName, String lastName, Long currentUserId) {
        String normalizedFirst = emailPart(firstName);
        String normalizedLast = emailPart(lastName);
        if (normalizedFirst.isBlank() || normalizedLast.isBlank()) {
            throw new BusinessException("No fue posible generar el correo con el nombre indicado.");
        }

        String base = normalizedFirst + normalizedLast.substring(0, 1);
        String candidate = base + TECHNICIAN_EMAIL_DOMAIN;
        int suffix = 2;
        while (emailBelongsToOtherUser(candidate, currentUserId)) {
            candidate = base + suffix + TECHNICIAN_EMAIL_DOMAIN;
            suffix++;
        }
        return candidate;
    }

    private boolean emailBelongsToOtherUser(String email, Long currentUserId) {
        return users.findByEmailIgnoreCase(email)
            .map(user -> currentUserId == null || !user.getId().equals(currentUserId))
            .orElse(false);
    }

    private String emailPart(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private String cleanName(String value, String fieldLabel) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (cleaned.length() < 2) {
            throw new BusinessException("Ingrese un " + fieldLabel + " válido.");
        }
        return cleaned;
    }

    private String cleanPhone(String value) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (!cleaned.matches("^[+0-9() -]{8,30}$")) {
            throw new BusinessException("Ingrese un número de teléfono válido.");
        }
        return cleaned;
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
