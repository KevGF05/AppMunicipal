package com.municipal.backend.repository;

import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    List<AppUser> findByRoleAndActiveTrueOrderByNameAsc(Role role);
    List<AppUser> findByRoleOrderByNameAsc(Role role);
    boolean existsByRole(Role role);
}
