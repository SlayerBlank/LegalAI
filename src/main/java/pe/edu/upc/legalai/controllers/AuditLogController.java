package pe.edu.upc.legalai.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDTO>> buscar(
            @RequestParam(required = false)
            Long userId,

            @RequestParam(required = false)
            String action,

            @RequestParam(required = false)
            String entityType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @PageableDefault(
                    sort = {"createdAt", "logId"},
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                auditLogService.buscar(
                        userId,
                        action,
                        entityType,
                        from,
                        to,
                        pageable
                )
        );
    }

    // HU-070 - Auditoría mediante filtros combinados.
    @GetMapping("/advanced-search")
    public ResponseEntity<Page<AuditLogResponseDTO>> buscarConsultaAcademica(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = {"createdAt", "logId"},
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.buscarConsultaAcademica(
                userId, action, entityType, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponseDTO> obtenerPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                auditLogService.obtenerPorId(id)
        );
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<AuditLogResponseDTO>> listarPorUsuario(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                auditLogService.listarPorUsuario(userId)
        );
    }
}