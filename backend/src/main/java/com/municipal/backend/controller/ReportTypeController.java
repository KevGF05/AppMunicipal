package com.municipal.backend.controller;

import com.municipal.backend.dto.ReportTypeCreateRequest;
import com.municipal.backend.dto.ReportTypeResponse;
import com.municipal.backend.dto.ReportTypeStatusRequest;
import com.municipal.backend.dto.ReportTypeUpdateRequest;
import com.municipal.backend.service.ReportTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ReportTypeController {
    private final ReportTypeService service;

    public ReportTypeController(ReportTypeService service) {
        this.service = service;
    }

    @GetMapping("/api/public/tipos-reporte")
    public List<ReportTypeResponse> publicTypes() {
        return service.listPublic();
    }

    @GetMapping("/api/admin/tipos-reporte")
    public List<ReportTypeResponse> adminTypes() {
        return service.listAdmin();
    }

    @PostMapping("/api/admin/tipos-reporte")
    public ReportTypeResponse createType(@Valid @RequestBody ReportTypeCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/admin/tipos-reporte/{id}")
    public ReportTypeResponse updateType(
        @PathVariable Long id,
        @Valid @RequestBody ReportTypeUpdateRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/api/admin/tipos-reporte/{id}/estado")
    public ReportTypeResponse changeTypeStatus(
        @PathVariable Long id,
        @RequestBody ReportTypeStatusRequest request
    ) {
        return service.setActive(id, request.active());
    }
}
