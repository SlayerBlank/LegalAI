package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.auditlog.AuditLogRequestDTO;
import pe.edu.upc.legalai.dtos.auditlog.AuditLogResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.AuditLogService;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Auditoría", description = "Operaciones de solo lectura y registro sobre la bitácora de auditoría")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Registrar entrada de auditoría", description = "Registra una nueva acción en la bitácora de auditoría")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrada registrada correctamente",
                    content = @Content(schema = @Schema(implementation = AuditLogResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AuditLogResponseDTO> create(@Valid @RequestBody AuditLogRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditLogService.create(request));
    }

    @Operation(summary = "Listar auditoría", description = "Obtiene el listado completo de la bitácora de auditoría")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<AuditLogResponseDTO>> findAll() {
        return ResponseEntity.ok(auditLogService.findAll());
    }

    @Operation(summary = "Obtener entrada de auditoría por ID", description = "Busca una entrada de auditoría por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada encontrada",
                    content = @Content(schema = @Schema(implementation = AuditLogResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Entrada no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponseDTO> findById(
            @Parameter(description = "Identificador de la entrada de auditoría", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.findById(id));
    }

    @Operation(summary = "Listar auditoría por usuario",
            description = "Obtiene las acciones registradas para un usuario específico, de la más reciente a la más antigua")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponseDTO>> findByUser(
            @Parameter(description = "Identificador del usuario", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.findByUserId(userId));
    }
}
