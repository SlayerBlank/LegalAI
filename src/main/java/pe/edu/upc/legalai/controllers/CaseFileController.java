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
import pe.edu.upc.legalai.dtos.casefile.CaseFileRequestDTO;
import pe.edu.upc.legalai.dtos.casefile.CaseFileResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.CaseFileService;

import java.util.List;

@RestController
@RequestMapping("/api/case-files")
@Tag(name = "Expedientes", description = "Operaciones relacionadas con la gestión de expedientes")
public class CaseFileController {

    private final CaseFileService caseFileService;

    public CaseFileController(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }

    @Operation(summary = "Crear expediente", description = "Registra un nuevo expediente asociado a un cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Expediente creado correctamente",
                    content = @Content(schema = @Schema(implementation = CaseFileResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CaseFileResponseDTO> create(@Valid @RequestBody CaseFileRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caseFileService.create(request));
    }

    @Operation(summary = "Listar expedientes", description = "Obtiene el listado completo de expedientes")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<CaseFileResponseDTO>> findAll() {
        return ResponseEntity.ok(caseFileService.findAll());
    }

    @Operation(summary = "Obtener expediente por ID", description = "Busca un expediente por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expediente encontrado",
                    content = @Content(schema = @Schema(implementation = CaseFileResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Expediente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CaseFileResponseDTO> findById(
            @Parameter(description = "Identificador del expediente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(caseFileService.findById(id));
    }

    @Operation(summary = "Listar expedientes por cliente",
            description = "Obtiene los expedientes registrados para un cliente específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<CaseFileResponseDTO>> findByClient(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long clientId) {
        return ResponseEntity.ok(caseFileService.findByClientId(clientId));
    }

    @Operation(summary = "Listar expedientes por usuario propietario",
            description = "Obtiene los expedientes registrados por un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CaseFileResponseDTO>> findByOwnerUser(
            @Parameter(description = "Identificador del usuario propietario", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(caseFileService.findByOwnerUserId(userId));
    }

    @Operation(summary = "Actualizar expediente", description = "Actualiza la información de un expediente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expediente actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = CaseFileResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Expediente, cliente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CaseFileResponseDTO> update(
            @Parameter(description = "Identificador del expediente", example = "1") @PathVariable Long id,
            @Valid @RequestBody CaseFileRequestDTO request) {
        return ResponseEntity.ok(caseFileService.update(id, request));
    }

    @Operation(summary = "Eliminar expediente", description = "Elimina un expediente por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Expediente eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Expediente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del expediente", example = "1") @PathVariable Long id) {
        caseFileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
