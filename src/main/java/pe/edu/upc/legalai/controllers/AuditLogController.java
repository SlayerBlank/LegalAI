package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(
        name = "Administration",
        description = "Consulta de auditoría exclusiva de administradores"
)
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(
            summary = "Buscar auditoría",
            description = "Lista la bitácora de auditoría de forma paginada, "
                    + "con filtros opcionales por usuario, acción, tipo de entidad "
                    + "y rango de fechas"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de resultados obtenida"
    )
    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDTO>> buscar(
            @Parameter(description = "Filtra por identificador de usuario")
            @RequestParam(required = false)
            Long userId,

            @Parameter(description = "Filtra por acción exacta")
            @RequestParam(required = false)
            String action,

            @Parameter(description = "Filtra por tipo de entidad")
            @RequestParam(required = false)
            String entityType,

            @Parameter(description = "Fecha/hora desde (inclusive)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fecha/hora hasta (inclusive)")
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

    @Operation(
            summary = "Obtener registro de auditoría por ID",
            description = "Busca un registro de auditoría por su identificador"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registro encontrado"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponseDTO> obtenerPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                auditLogService.obtenerPorId(id)
        );
    }

    @Operation(
            summary = "Listar auditoría por usuario",
            description = "Obtiene las acciones registradas para un usuario, "
                    + "ordenadas de forma descendente"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido"
    )
    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<AuditLogResponseDTO>> listarPorUsuario(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                auditLogService.listarPorUsuario(userId)
        );
    }
}