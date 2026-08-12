package com.municipal.backend.repository;

import com.municipal.backend.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportTypeRepository extends JpaRepository<ReportType, Long> {
    List<ReportType> findByActiveTrueOrderByNameAsc();
    List<ReportType> findAllByOrderByNameAsc();
    Optional<ReportType> findByNameIgnoreCase(String name);
    Optional<ReportType> findByNameIgnoreCaseAndActiveTrue(String name);
    boolean existsByNameIgnoreCase(String name);
}
