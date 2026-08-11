package com.municipal.backend.repository;

import com.municipal.backend.model.Report;
import com.municipal.backend.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByCode(String code);
    boolean existsByCode(String code);
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findByAssignedTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    boolean existsByAssignedTechnicianIdAndStatusIn(Long technicianId, List<ReportStatus> statuses);
}
