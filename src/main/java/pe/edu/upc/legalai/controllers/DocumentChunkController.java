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
import pe.edu.upc.legalai.dtos.documentchunk.DocumentChunkRequestDTO;
import pe.edu.upc.legalai.dtos.documentchunk.DocumentChunkResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.DocumentChunkService;

import java.util.List;

@RestController
@RequestMapping("/api/document-chunks")
@Tag(name = "Fragmentos de documentos", description = "Operaciones relacionadas con los fragmentos usados para búsqueda semántica (RAG)")
public class DocumentChunkController {

    private final DocumentChunkService documentChunkService;

    public DocumentChunkController(DocumentChunkService documentChunkService) {
        this.documentChunkService = documentChunkService;
    }

    @Operation(summary = "Crear fragmento", description = "Registra un nuevo fragmento de texto de un documento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fragmento creado correctamente",
                    content = @Content(schema = @Schema(implementation = DocumentChunkResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DocumentChunkResponseDTO> create(@Valid @RequestBody DocumentChunkRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentChunkService.create(request));
    }

    @Operation(summary = "Listar fragmentos", description = "Obtiene el listado completo de fragmentos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<DocumentChunkResponseDTO>> findAll() {
        return ResponseEntity.ok(documentChunkService.findAll());
    }

    @Operation(summary = "Obtener fragmento por ID", description = "Busca un fragmento por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fragmento encontrado",
                    content = @Content(schema = @Schema(implementation = DocumentChunkResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Fragmento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentChunkResponseDTO> findById(
            @Parameter(description = "Identificador del fragmento", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(documentChunkService.findById(id));
    }

    @Operation(summary = "Listar fragmentos por documento",
            description = "Obtiene los fragmentos de un documento específico, ordenados por posición")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<DocumentChunkResponseDTO>> findByDocument(
            @Parameter(description = "Identificador del documento", example = "1") @PathVariable Long documentId) {
        return ResponseEntity.ok(documentChunkService.findByDocumentId(documentId));
    }

    @Operation(summary = "Eliminar fragmento", description = "Elimina un fragmento por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fragmento eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Fragmento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del fragmento", example = "1") @PathVariable Long id) {
        documentChunkService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
