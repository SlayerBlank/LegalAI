package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.DTOs.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Auditoria", description = "Consulta de la bitacora de auditoria (registro de solo lectura)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Listar auditoria", description = "Obtiene el listado completo de la bitacora de auditoria")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<AuditLogResponseDTO>> listar() {
        return ResponseEntity.ok(auditLogService.listar());
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
