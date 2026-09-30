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
import pe.edu.upc.legalai.dtos.draft.DraftRequestDTO;
import pe.edu.upc.legalai.dtos.draft.DraftResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.DraftService;

import java.util.List;

@RestController
@RequestMapping("/api/drafts")
@Tag(name = "Borradores", description = "Operaciones relacionadas con los borradores jurídicos generados con apoyo de IA")
public class DraftController {

    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    @Operation(summary = "Crear borrador", description = "Registra un nuevo borrador jurídico asociado a un expediente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Borrador creado correctamente",
                    content = @Content(schema = @Schema(implementation = DraftResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Expediente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DraftResponseDTO> create(@Valid @RequestBody DraftRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.create(request));
    }

    @Operation(summary = "Listar borradores", description = "Obtiene el listado completo de borradores")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<DraftResponseDTO>> findAll() {
        return ResponseEntity.ok(draftService.findAll());
    }

    @Operation(summary = "Obtener borrador por ID", description = "Busca un borrador por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Borrador encontrado",
                    content = @Content(schema = @Schema(implementation = DraftResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Borrador no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DraftResponseDTO> findById(
            @Parameter(description = "Identificador del borrador", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(draftService.findById(id));
    }

    @Operation(summary = "Listar borradores por expediente",
            description = "Obtiene los borradores registrados para un expediente específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Expediente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<DraftResponseDTO>> findByCase(
            @Parameter(description = "Identificador del expediente", example = "1") @PathVariable Long caseId) {
        return ResponseEntity.ok(draftService.findByCaseId(caseId));
    }

    @Operation(summary = "Actualizar borrador", description = "Actualiza el contenido o estado de un borrador existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Borrador actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = DraftResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Borrador no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DraftResponseDTO> update(
            @Parameter(description = "Identificador del borrador", example = "1") @PathVariable Long id,
            @Valid @RequestBody DraftRequestDTO request) {
        return ResponseEntity.ok(draftService.update(id, request));
    }

    @Operation(summary = "Eliminar borrador", description = "Elimina un borrador por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Borrador eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Borrador no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del borrador", example = "1") @PathVariable Long id) {
        draftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
