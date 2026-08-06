package com.municipal.backend.repository;

import com.municipal.backend.model.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    List<ReportHistory> findByReportIdOrderByCreatedAtAsc(Long reportId);
}
