package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import pe.edu.upc.legalai.exceptions.ErrorResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.RolRequestDTO;
import pe.edu.upc.legalai.dtos.response.RolResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IRolService;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "rol-controller", description = "Administracion exclusiva de ROLE_ADMIN. ADMIN y USER son roles fundamentales.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Entrada invalida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT ausente, invalido o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class RolController {
    private final IRolService rolService;

    public RolController(IRolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @Operation(summary = "Listar roles")
    @ApiResponse(responseCode = "200", description = "Roles registrados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RolResponseDTO.class))))
    public ResponseEntity<List<RolResponseDTO>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }

    @PostMapping
    @Operation(summary = "Registrar rol", description = "Normaliza a mayusculas; no admite nombres reservados ni prefijo ROLE_. No asigna el rol a ningun usuario.")
    @ApiResponse(responseCode = "201", description = "Rol creado", content = @Content(schema = @Schema(implementation = RolResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Nombre duplicado o reservado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RolResponseDTO> registrar(@Valid @RequestBody RolRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.registrar(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar rol")
    @ApiResponse(responseCode = "200", description = "Rol encontrado", content = @Content(schema = @Schema(implementation = RolResponseDTO.class)))
    public ResponseEntity<RolResponseDTO> buscarPorId(
            @Parameter(description = "Identificador positivo del rol") @PathVariable Long id) {
        return ResponseEntity.ok(rolService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar rol", description = "Los roles fundamentales o asignados solo permiten cambiar la descripcion, conservando su nombre.")
    @ApiResponse(responseCode = "200", description = "Rol actualizado", content = @Content(schema = @Schema(implementation = RolResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Nombre duplicado, reservado o rol asignado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RolResponseDTO> actualizar(
            @Parameter(description = "Identificador positivo del rol") @PathVariable Long id,
            @Valid @RequestBody RolRequestDTO request) {
        return ResponseEntity.ok(rolService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar rol", description = "Solo roles no fundamentales y sin usuarios asociados, incluidos usuarios inactivos.")
    @ApiResponse(responseCode = "204", description = "Rol eliminado", content = @Content)
    @ApiResponse(responseCode = "409", description = "Rol fundamental, asignado o restriccion de integridad", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador positivo del rol") @PathVariable Long id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
