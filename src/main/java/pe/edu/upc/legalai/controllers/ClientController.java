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
import pe.edu.upc.legalai.dtos.client.ClientRequestDTO;
import pe.edu.upc.legalai.dtos.client.ClientResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.ClientService;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clientes", description = "Operaciones relacionadas con la gestión de clientes")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente asociado a un usuario propietario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado correctamente",
                    content = @Content(schema = @Schema(implementation = ClientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario propietario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(@Valid @RequestBody ClientRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @Operation(summary = "Listar clientes", description = "Obtiene el listado completo de clientes")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> findAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @Operation(summary = "Obtener cliente por ID", description = "Busca un cliente por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClientResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> findById(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @Operation(summary = "Listar clientes por usuario",
            description = "Obtiene los clientes registrados por un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientResponseDTO>> findByOwnerUser(
            @Parameter(description = "Identificador del usuario propietario", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(clientService.findByOwnerUserId(userId));
    }

    @Operation(summary = "Actualizar cliente", description = "Actualiza la información de un cliente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = ClientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
