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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.role.RoleRequestDTO;
import pe.edu.upc.legalai.dtos.role.RoleResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.RoleService;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Operaciones relacionadas con la gestión de roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Crear rol", description = "Registra un nuevo rol en la plataforma")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rol creado correctamente",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un rol con el mismo nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RoleResponseDTO> create(@Valid @RequestBody RoleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    @Operation(summary = "Listar roles", description = "Obtiene el listado completo de roles")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @Operation(summary = "Obtener rol por ID", description = "Busca un rol por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol encontrado",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> findById(
            @Parameter(description = "Identificador del rol", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @Operation(summary = "Actualizar rol", description = "Actualiza la información de un rol existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = RoleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un rol con el mismo nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> update(
            @Parameter(description = "Identificador del rol", example = "1") @PathVariable Long id,
            @Valid @RequestBody RoleRequestDTO request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @Operation(summary = "Eliminar rol", description = "Elimina un rol por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rol eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El rol está asociado a usuarios",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del rol", example = "1") @PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
