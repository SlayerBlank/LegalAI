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
import pe.edu.upc.legalai.dtos.document.DocumentRequestDTO;
import pe.edu.upc.legalai.dtos.document.DocumentResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.DocumentService;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documentos", description = "Operaciones relacionadas con la gestión de documentos")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Crear documento", description = "Registra un nuevo documento asociado al usuario que lo sube")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento creado correctamente",
                    content = @Content(schema = @Schema(implementation = DocumentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DocumentResponseDTO> create(@Valid @RequestBody DocumentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(request));
    }

    @Operation(summary = "Listar documentos", description = "Obtiene el listado completo de documentos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> findAll() {
        return ResponseEntity.ok(documentService.findAll());
    }

    @Operation(summary = "Obtener documento por ID", description = "Busca un documento por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento encontrado",
                    content = @Content(schema = @Schema(implementation = DocumentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> findById(
            @Parameter(description = "Identificador del documento", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    @Operation(summary = "Listar documentos por usuario",
            description = "Obtiene los documentos subidos por un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentResponseDTO>> findByUploadedBy(
            @Parameter(description = "Identificador del usuario que subió los documentos", example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(documentService.findByUploadedByUserId(userId));
    }

    @Operation(summary = "Actualizar documento", description = "Actualiza la información de un documento existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = DocumentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> update(
            @Parameter(description = "Identificador del documento", example = "1") @PathVariable Long id,
            @Valid @RequestBody DocumentRequestDTO request) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @Operation(summary = "Eliminar documento", description = "Elimina un documento por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documento eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del documento", example = "1") @PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
