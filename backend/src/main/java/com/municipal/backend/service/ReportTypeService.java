package com.municipal.backend.service;

import com.municipal.backend.dto.ReportTypeCreateRequest;
import com.municipal.backend.dto.ReportTypeResponse;
import com.municipal.backend.exception.BusinessException;
import com.municipal.backend.model.ReportType;
import com.municipal.backend.repository.ReportTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReportTypeService {
    private final ReportTypeRepository types;

    public ReportTypeService(ReportTypeRepository types) {
        this.types = types;
    }

    @Transactional(readOnly = true)
    public List<ReportTypeResponse> listPublic() {
        return types.findByActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportTypeResponse> listAdmin() {
        return types.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportTypeResponse create(ReportTypeCreateRequest request) {
        String name = normalize(request.name());
        if (types.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Ese tipo de reporte ya existe.");
        }
        ReportType type = new ReportType();
        type.setName(name);
        type.setActive(true);
        return toResponse(types.save(type));
    }

    @Transactional(readOnly = true)
    public boolean isActiveType(String value) {
        if (value == null || value.isBlank()) return false;
        return types.findByNameIgnoreCaseAndActiveTrue(normalize(value)).isPresent();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private ReportTypeResponse toResponse(ReportType type) {
        return new ReportTypeResponse(type.getId(), type.getName(), type.isActive());
    }
}
