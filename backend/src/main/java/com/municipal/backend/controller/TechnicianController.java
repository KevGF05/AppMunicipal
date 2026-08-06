package com.municipal.backend.controller;

import com.municipal.backend.dto.ReportResponse;
import com.municipal.backend.model.AppUser;
import com.municipal.backend.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/tecnico")
public class TechnicianController {
    private final ReportService reportService;

    public TechnicianController(ReportService reportService) { this.reportService = reportService; }

    @GetMapping("/reportes")
    public List<ReportResponse> list(Authentication authentication) {
        return reportService.listTechnician(user(authentication));
    }

    @GetMapping("/reportes/{id}")
    public ReportResponse get(@PathVariable Long id, Authentication authentication) {
        return reportService.getTechnician(id, user(authentication));
    }

    @PatchMapping("/reportes/{id}/iniciar")
    public ReportResponse start(@PathVariable Long id, Authentication authentication) {
        return reportService.start(id, user(authentication));
    }

    @PostMapping(value = "/reportes/{id}/cerrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportResponse close(
        @PathVariable Long id,
        @RequestParam String comentario,
        @RequestPart("fotoFinal") MultipartFile fotoFinal,
        Authentication authentication
    ) {
        return reportService.close(id, comentario, fotoFinal, user(authentication));
    }

    private AppUser user(Authentication authentication) { return (AppUser) authentication.getPrincipal(); }
}
