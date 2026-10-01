package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Administration", description = "Consulta de auditoria exclusiva de administradores")
public class AuditLogController {
    private final AuditLogService audit;

    public AuditLogController(AuditLogService audit) { this.audit = audit; }

    @GetMapping
    @Operation(summary = "Buscar auditoria", description = "Filtros opcionales; fechas inclusivas y paginacion en base de datos")
    public Page<AuditLogResponseDTO> buscar(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(sort = {"createdAt", "logId"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return audit.buscar(userId, action, entityType, from, to, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un registro de auditoria")
    public AuditLogResponseDTO obtener(@PathVariable Long id) { return audit.obtenerPorId(id); }

    @GetMapping("/usuario/{userId}")
    @Operation(summary = "Consultar auditoria de un usuario", description = "Orden descendente por fecha e identificador")
    public List<AuditLogResponseDTO> listar(@PathVariable Long userId) { return audit.listarPorUsuario(userId); }
}
