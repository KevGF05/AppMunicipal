package com.municipal.backend.controller;

import com.municipal.backend.dto.CreateReportResponse;
import com.municipal.backend.dto.TrackingResponse;
import com.municipal.backend.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/public/reportes")
public class PublicReportController {
    private final ReportService reportService;

    public PublicReportController(ReportService reportService) { this.reportService = reportService; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CreateReportResponse create(
        @RequestParam String tipo,
        @RequestParam String descripcion,
        @RequestParam String direccion,
        @RequestParam double latitud,
        @RequestParam double longitud,
        @RequestPart("foto") MultipartFile foto
    ) {
        return reportService.createAnonymous(tipo, descripcion, direccion, latitud, longitud, foto);
    }

    @GetMapping("/{codigo}")
    public TrackingResponse track(@PathVariable String codigo) {
        return reportService.track(codigo);
    }
}
