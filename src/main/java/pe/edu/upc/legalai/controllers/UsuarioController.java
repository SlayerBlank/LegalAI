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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.UsuarioUpdateRequestDTO;
import pe.edu.upc.legalai.dtos.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "usuario-controller", description = "Perfil autenticado y administracion de usuarios. Registro mediante /api/auth/register.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Entrada invalida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT ausente, invalido o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "403", description = "Se requiere ROLE_ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class UsuarioController {
    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar mi perfil", description = "Disponible para cualquier usuario activo autenticado.")
    @ApiResponse(responseCode = "200", description = "Perfil sin credenciales", content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class)))
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(usuarioService.obtenerPerfilAutenticado());
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Solo ADMIN. Incluye usuarios activos e inactivos; orden por userId.")
    @ApiResponse(responseCode = "200", description = "Usuarios sin credenciales", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioResponseDTO.class))))
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario", description = "Solo ADMIN.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado", content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class)))
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @Parameter(description = "Identificador positivo del usuario") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar perfil de usuario", description = "Solo ADMIN. Actualiza nombre; exige conservar el correo usado como identidad JWT. No cambia rol, estado ni contrasena.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado", content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Correo asociado a otro usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @Parameter(description = "Identificador positivo del usuario") @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequestDTO request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja usuario", description = "Solo ADMIN. Baja logica INACTIVE: conserva datos y correo; JWT deja de autenticar. No permite dar de baja administradores ni la cuenta actual. Idempotente para usuarios ya inactivos.")
    @ApiResponse(responseCode = "204", description = "Usuario inactivo", content = @Content)
    @ApiResponse(responseCode = "409", description = "Cuenta administrativa protegida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador positivo del usuario") @PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
