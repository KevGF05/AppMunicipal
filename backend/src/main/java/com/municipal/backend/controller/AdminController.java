package com.municipal.backend.controller;

import com.municipal.backend.dto.*;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.model.ReportStatus;
import com.municipal.backend.service.ReportService;
import com.municipal.backend.service.TechnicianManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ReportService reportService;
    private final TechnicianManagementService technicianService;

    public AdminController(
        ReportService reportService,
        TechnicianManagementService technicianService
    ) {
        this.reportService = reportService;
        this.technicianService = technicianService;
    }

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

    @PatchMapping("/reportes/{id}/reasignar")
    public ReportResponse reassign(
        @PathVariable Long id,
        @Valid @RequestBody AssignRequest request,
        Authentication authentication
    ) {
        return reportService.reassign(id, request, user(authentication));
    }

    @GetMapping("/tecnicos")
    public List<TechnicianResponse> technicians(
        @RequestParam(defaultValue = "false") boolean soloActivos
    ) {
        return technicianService.list(soloActivos);
    }

    @PostMapping("/tecnicos")
    public TechnicianResponse createTechnician(
        @Valid @RequestBody TechnicianCreateRequest request
    ) {
        return technicianService.create(request);
    }

    @PutMapping("/tecnicos/{id}")
    public TechnicianResponse updateTechnician(
        @PathVariable Long id,
        @Valid @RequestBody TechnicianUpdateRequest request
    ) {
        return technicianService.update(id, request);
    }

    @PatchMapping("/tecnicos/{id}/estado")
    public TechnicianResponse changeTechnicianStatus(
        @PathVariable Long id,
        @Valid @RequestBody TechnicianStatusRequest request
    ) {
        return technicianService.setActive(id, request.active());
    }

    @PatchMapping("/tecnicos/{id}/reset-password")
    public TechnicianResponse resetTechnicianPassword(@PathVariable Long id) {
        return technicianService.resetPassword(id);
    }

    private AppUser user(Authentication authentication) { return (AppUser) authentication.getPrincipal(); }
}
