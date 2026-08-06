package com.municipal.backend.controller;

import com.municipal.backend.dto.*;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.ReportStatus;
import com.municipal.backend.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ReportService reportService;

    public AdminController(ReportService reportService) { this.reportService = reportService; }

    @GetMapping("/reportes")
    public List<ReportResponse> list(@RequestParam(required = false) ReportStatus estado) {
        return reportService.listAdmin(estado);
    }

    @GetMapping("/reportes/{id}")
    public ReportResponse get(@PathVariable Long id) { return reportService.getAdmin(id); }

    @PatchMapping("/reportes/{id}/aprobar")
    public ReportResponse approve(@PathVariable Long id, Authentication authentication) {
        return reportService.approve(id, user(authentication));
    }

    @PatchMapping("/reportes/{id}/rechazar")
    public ReportResponse reject(
        @PathVariable Long id,
        @Valid @RequestBody RejectRequest request,
        Authentication authentication
    ) {
        return reportService.reject(id, request.reason(), user(authentication));
    }

    @PatchMapping("/reportes/{id}/asignar")
    public ReportResponse assign(
        @PathVariable Long id,
        @Valid @RequestBody AssignRequest request,
        Authentication authentication
    ) {
        return reportService.assign(id, request, user(authentication));
    }

    @GetMapping("/tecnicos")
    public List<TechnicianResponse> technicians() { return reportService.technicians(); }

    private AppUser user(Authentication authentication) { return (AppUser) authentication.getPrincipal(); }
}
