package com.municipal.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_reportes")
public class ReportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false)
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReportStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus newStatus;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    public ReportStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ReportStatus previousStatus) { this.previousStatus = previousStatus; }
    public ReportStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ReportStatus newStatus) { this.newStatus = newStatus; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
