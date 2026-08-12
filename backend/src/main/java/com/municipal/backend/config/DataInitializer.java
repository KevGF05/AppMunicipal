package com.municipal.backend.config;

import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.Role;
import com.municipal.backend.model.ReportType;
import com.municipal.backend.repository.AppUserRepository;
import com.municipal.backend.repository.ReportTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.text.Normalizer;
import java.util.Locale;

@Configuration
public class DataInitializer {
    private static final String TECHNICIAN_EMAIL_DOMAIN = "@pmunicipalidad.hn";

    @Value("${APP_ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${APP_ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${APP_TECNICO1_PASSWORD:Tecnico123*}")
    private String technicianPassword;

    @Bean
    CommandLineRunner seedUsers(AppUserRepository users, PasswordEncoder encoder) {
        return args -> {
            createAdminIfMissing(users, encoder);
            createDefaultTechnicianIfNeeded(users, encoder);
            migrateExistingTechnicians(users);
        };
    }



    @Bean
    CommandLineRunner seedReportTypes(ReportTypeRepository types) {
        return args -> {
            if (types.count() > 0) return;
            createReportTypeIfMissing(types, "Señal de tránsito caída");
            createReportTypeIfMissing(types, "Señal de tránsito dañada");
            createReportTypeIfMissing(types, "Semáforo dañado");
            createReportTypeIfMissing(types, "Automóvil mal estacionado");
        };
    }

    private void createReportTypeIfMissing(ReportTypeRepository types, String name) {
        if (types.existsByNameIgnoreCase(name)) return;
        ReportType type = new ReportType();
        type.setName(name);
        type.setActive(true);
        types.save(type);
    }

    private void createAdminIfMissing(AppUserRepository users, PasswordEncoder encoder) {
        AppUser existing = users.findByEmailIgnoreCase(adminEmail).orElse(null);
        if (existing != null) {
            if (existing.getPasswordChangeRequired() == null) {
                existing.setPasswordChangeRequired(false);
                users.save(existing);
            }
            return;
        }
        AppUser user = new AppUser();
        user.setFirstName("Administrador");
        user.setLastName("Municipal");
        user.setName("Administrador municipal");
        user.setEmail(adminEmail.trim().toLowerCase());
        user.setPasswordHash(encoder.encode(adminPassword));
        user.setPasswordChangeRequired(false);
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);
        users.save(user);
    }

    private void createDefaultTechnicianIfNeeded(AppUserRepository users, PasswordEncoder encoder) {
        if (users.existsByRole(Role.TECNICO)) return;
        AppUser user = new AppUser();
        user.setFirstName("Carlos");
        user.setLastName("Mejía");
        user.setName("Carlos Mejía");
        user.setEmail("carlosm" + TECHNICIAN_EMAIL_DOMAIN);
        user.setPasswordHash(encoder.encode(technicianPassword));
        user.setPasswordChangeRequired(true);
        user.setRole(Role.TECNICO);
        user.setActive(true);
        users.save(user);
    }

    private void migrateExistingTechnicians(AppUserRepository users) {
        for (AppUser technician : users.findByRoleOrderByNameAsc(Role.TECNICO)) {
            boolean changed = false;
            String firstName = technician.getFirstName();
            String lastName = technician.getLastName();
            if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                String[] parts = splitName(technician.getName());
                firstName = parts[0];
                lastName = parts[1];
                technician.setFirstName(firstName);
                technician.setLastName(lastName);
                technician.setName(firstName + " " + lastName);
                changed = true;
            }

            if (technician.getEmail() == null || !technician.getEmail().toLowerCase(Locale.ROOT).endsWith(TECHNICIAN_EMAIL_DOMAIN)) {
                technician.setEmail(generateUniqueEmail(users, firstName, lastName, technician.getId()));
                changed = true;
            }

            if (technician.getPasswordChangeRequired() == null) {
                technician.setPasswordChangeRequired(true);
                changed = true;
            }

            if (changed) users.save(technician);
        }
    }

    private String[] splitName(String fullName) {
        String full = fullName == null ? "" : fullName.trim();
        if (full.isBlank()) return new String[] { "Tecnico", "Municipal" };
        String[] pieces = full.split("\\s+");
        if (pieces.length == 1) return new String[] { pieces[0], "Municipal" };
        return new String[] { pieces[0], pieces[pieces.length - 1] };
    }

    private String generateUniqueEmail(AppUserRepository users, String firstName, String lastName, Long currentId) {
        String normalizedFirst = emailPart(firstName);
        String normalizedLast = emailPart(lastName);
        String base = normalizedFirst + normalizedLast.substring(0, 1);
        String candidate = base + TECHNICIAN_EMAIL_DOMAIN;
        int suffix = 2;
        while (belongsToAnotherUser(users, candidate, currentId)) {
            candidate = base + suffix + TECHNICIAN_EMAIL_DOMAIN;
            suffix++;
        }
        return candidate;
    }

    private boolean belongsToAnotherUser(AppUserRepository users, String email, Long currentId) {
        return users.findByEmailIgnoreCase(email)
            .map(user -> currentId == null || !user.getId().equals(currentId))
            .orElse(false);
    }

    private String emailPart(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }
}
