package com.municipal.backend.config;

import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.Role;
import com.municipal.backend.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(AppUserRepository users, PasswordEncoder encoder) {
        return args -> {
            createIfMissing(users, encoder, "Administrador municipal", "admin@municipalidad.hn", "Admin123*", Role.ADMINISTRADOR);
            createIfMissing(users, encoder, "Técnico vial 1", "tecnico1@municipalidad.hn", "Tecnico123*", Role.TECNICO);
            createIfMissing(users, encoder, "Técnico vial 2", "tecnico2@municipalidad.hn", "Tecnico123*", Role.TECNICO);
        };
    }

    private void createIfMissing(
        AppUserRepository users,
        PasswordEncoder encoder,
        String name,
        String email,
        String password,
        Role role
    ) {
        if (users.findByEmailIgnoreCase(email).isPresent()) return;
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        users.save(user);
    }
}
