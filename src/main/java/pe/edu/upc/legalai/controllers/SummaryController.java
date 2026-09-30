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
import pe.edu.upc.legalai.dtos.summary.SummaryRequestDTO;
import pe.edu.upc.legalai.dtos.summary.SummaryResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.SummaryService;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@Tag(name = "Resúmenes", description = "Operaciones relacionadas con los resúmenes generados por IA sobre documentos o expedientes")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @Operation(summary = "Crear resumen", description = "Registra un nuevo resumen generado por IA para un documento o expediente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resumen creado correctamente",
                    content = @Content(schema = @Schema(implementation = SummaryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento, expediente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SummaryResponseDTO> create(@Valid @RequestBody SummaryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(summaryService.create(request));
    }

    @Operation(summary = "Listar resúmenes", description = "Obtiene el listado completo de resúmenes")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<SummaryResponseDTO>> findAll() {
        return ResponseEntity.ok(summaryService.findAll());
    }

    @Operation(summary = "Obtener resumen por ID", description = "Busca un resumen por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen encontrado",
                    content = @Content(schema = @Schema(implementation = SummaryResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Resumen no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SummaryResponseDTO> findById(
            @Parameter(description = "Identificador del resumen", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(summaryService.findById(id));
    }

    @Operation(summary = "Listar resúmenes por documento",
            description = "Obtiene los resúmenes generados para un documento específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<SummaryResponseDTO>> findByDocument(
            @Parameter(description = "Identificador del documento", example = "1") @PathVariable Long documentId) {
        return ResponseEntity.ok(summaryService.findByDocumentId(documentId));
    }

    @Operation(summary = "Listar resúmenes por expediente",
            description = "Obtiene los resúmenes generados para un expediente específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Expediente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<SummaryResponseDTO>> findByCase(
            @Parameter(description = "Identificador del expediente", example = "1") @PathVariable Long caseId) {
        return ResponseEntity.ok(summaryService.findByCaseId(caseId));
    }

    @Operation(summary = "Eliminar resumen", description = "Elimina un resumen por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resumen eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Resumen no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del resumen", example = "1") @PathVariable Long id) {
        summaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
