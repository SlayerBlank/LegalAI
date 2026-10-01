package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Auditoria", description = "Consulta de la bitacora de auditoria (registro de solo lectura)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Buscar auditoria", description = "Lista la bitacora de auditoria de forma paginada, "
            + "con filtros opcionales por usuario, accion, tipo de entidad y rango de fechas")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados obtenida")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDTO>> buscar(
            @Parameter(description = "Filtra por identificador de usuario") @RequestParam(required = false) Long userId,
            @Parameter(description = "Filtra por accion exacta (ej. CREATE_CLIENT)") @RequestParam(required = false) String action,
            @Parameter(description = "Filtra por tipo de entidad (ej. Cliente)") @RequestParam(required = false) String entityType,
            @Parameter(description = "Fecha/hora desde (inclusive), formato ISO-8601") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha/hora hasta (inclusive), formato ISO-8601") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Paginacion: page, size y sort (ej. sort=createdAt,desc)") Pageable pageable) {
        return ResponseEntity.ok(auditLogService.buscar(userId, action, entityType, from, to, pageable));
    }

    @Operation(summary = "Obtener registro de auditoria por ID", description = "Busca un registro de auditoria por su identificador")
    @ApiResponse(responseCode = "200", description = "Registro encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.obtenerPorId(id));
    }

    @Operation(summary = "Listar auditoria por usuario", description = "Obtiene las acciones registradas por un usuario, de la mas reciente a la mas antigua")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<AuditLogResponseDTO>> listarPorUsuario(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.listarPorUsuario(userId));
    }
}
