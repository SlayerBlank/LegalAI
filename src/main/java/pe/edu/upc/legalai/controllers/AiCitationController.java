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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.aicitation.AiCitationRequestDTO;
import pe.edu.upc.legalai.dtos.aicitation.AiCitationResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.AiCitationService;

import java.util.List;

@RestController
@RequestMapping("/api/ai-citations")
@Tag(name = "Citas de IA", description = "Operaciones relacionadas con las citas documentales generadas por la IA en el chat")
public class AiCitationController {

    private final AiCitationService aiCitationService;

    public AiCitationController(AiCitationService aiCitationService) {
        this.aiCitationService = aiCitationService;
    }

    @Operation(summary = "Registrar cita", description = "Registra una nueva cita de un documento generada por la IA")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cita registrada correctamente",
                    content = @Content(schema = @Schema(implementation = AiCitationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje, documento o fragmento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AiCitationResponseDTO> create(@Valid @RequestBody AiCitationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiCitationService.create(request));
    }

    @Operation(summary = "Listar citas", description = "Obtiene el listado completo de citas generadas por la IA")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<AiCitationResponseDTO>> findAll() {
        return ResponseEntity.ok(aiCitationService.findAll());
    }

    @Operation(summary = "Obtener cita por ID", description = "Busca una cita por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita encontrada",
                    content = @Content(schema = @Schema(implementation = AiCitationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AiCitationResponseDTO> findById(
            @Parameter(description = "Identificador de la cita", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(aiCitationService.findById(id));
    }

    @Operation(summary = "Listar citas por mensaje",
            description = "Obtiene las citas asociadas a un mensaje de chat específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/message/{messageId}")
    public ResponseEntity<List<AiCitationResponseDTO>> findByMessage(
            @Parameter(description = "Identificador del mensaje", example = "1") @PathVariable Long messageId) {
        return ResponseEntity.ok(aiCitationService.findByMessageId(messageId));
    }

    @Operation(summary = "Eliminar cita", description = "Elimina una cita por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cita eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador de la cita", example = "1") @PathVariable Long id) {
        aiCitationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
