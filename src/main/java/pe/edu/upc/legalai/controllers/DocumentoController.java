package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.upc.legalai.dtos.request.DocumentoUpdateRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoPendienteResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoPreparationResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoTextResponseDTO;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoDownloadService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoProcessingService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "documento-controller", description = "Documentos PDF del usuario autenticado. Un documento ajeno responde 404.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Entrada invalida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT ausente, invalido o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Documento inexistente o de otro usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class DocumentoController {

    private final IDocumentoService documentoService;
    private final DocumentoProcessingService processingService;
    private final DocumentoDownloadService downloads;
    private final DocumentoPreparationService preparation;

    public DocumentoController(
            IDocumentoService documentoService,
            DocumentoProcessingService processingService,
            DocumentoDownloadService downloads,
            DocumentoPreparationService preparation
    ) {
        this.documentoService = documentoService;
        this.processingService = processingService;
        this.downloads = downloads;
        this.preparation = preparation;
    }

    @PostMapping("/{documentId}/prepare")
    @Operation(summary = "Preparar documento para IA (HU-029)",
            description = "Extrae o reutiliza el texto, genera fragmentos y embeddings. Si falla una etapa, "
                    + "el documento queda en ERROR y el fallo se registra en auditoria.")
    @ApiResponse(responseCode = "200", description = "Estado de preparacion", content = @Content(schema = @Schema(implementation = DocumentoPreparationResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "El documento ya se esta procesando", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Servicio de embeddings no disponible", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DocumentoPreparationResponseDTO preparar(
            @Parameter(description = "ID del documento") @PathVariable Long documentId
    ) {
        return preparation.preparar(documentId);
    }

    @GetMapping("/{documentId}/preparation")
    @Operation(summary = "Consultar estado de preparacion para IA")
    @ApiResponse(responseCode = "200", description = "Estado de preparacion", content = @Content(schema = @Schema(implementation = DocumentoPreparationResponseDTO.class)))
    public DocumentoPreparationResponseDTO estadoPreparacion(
            @Parameter(description = "ID del documento") @PathVariable Long documentId
    ) {
        return preparation.estado(documentId);
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Descargar PDF original (HU-027)",
            description = "Devuelve el archivo como application/pdf con Content-Disposition attachment.")
    @ApiResponse(responseCode = "200", description = "Archivo PDF", content = @Content(mediaType = "application/pdf"))
    public ResponseEntity<Resource> descargar(
            @Parameter(description = "ID del documento") @PathVariable Long documentId
    ) {
        var file = downloads.descargar(documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        file.fileName(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.resource());
    }

    @PostMapping("/{documentId}/process")
    @Operation(summary = "Procesar PDF y extraer texto (HU-058)",
            description = "Extrae el texto con PDFBox. Un PDF sin texto (escaneado) queda en ERROR y responde 400. "
                    + "Un documento en PROCESSING por mas de 10 minutos se puede reintentar.")
    @ApiResponse(responseCode = "200", description = "Documento procesado", content = @Content(schema = @Schema(implementation = DocumentoResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "El documento ya se esta procesando", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "No se pudo procesar el PDF", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<DocumentoResponseDTO> procesar(
            @Parameter(description = "ID del documento") @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                processingService.procesar(documentId)
        );
    }

    @GetMapping("/{documentId}/text")
    @Operation(summary = "Consultar texto extraido (HU-028)",
            description = "Si el documento aun no tiene texto, text es null y processingStatus y message indican por que.")
    @ApiResponse(responseCode = "200", description = "Texto o estado del documento", content = @Content(schema = @Schema(implementation = DocumentoTextResponseDTO.class)))
    public ResponseEntity<DocumentoTextResponseDTO> obtenerTexto(
            @Parameter(description = "ID del documento") @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                processingService.obtenerTexto(documentId)
        );
    }

    @GetMapping("/pending-review/{abogadoId}")
    @Operation(summary = "Listar documentos pendientes de revision por abogado (HU-67)",
            description = "Pendientes = UPLOADED, PROCESSING o ERROR. Solo el propio abogado o un ADMIN; "
                    + "para otro usuario o un abogado inexistente responde 404.")
    @ApiResponse(responseCode = "200", description = "Documentos pendientes (lista vacia si no hay)", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentoPendienteResponseDTO.class))))
    public ResponseEntity<List<DocumentoPendienteResponseDTO>> listarPendientesRevision(
            @Parameter(description = "ID del abogado") @PathVariable Long abogadoId,
            @Parameter(description = "Filtra por un estado pendiente: UPLOADED, PROCESSING o ERROR")
            @RequestParam(required = false) EstadoProcesamiento status
    ) {
        return ResponseEntity.ok(
                documentoService.listarPendientesRevision(abogadoId, status)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar metadatos de un documento")
    @ApiResponse(responseCode = "200", description = "Documento encontrado", content = @Content(schema = @Schema(implementation = DocumentoResponseDTO.class)))
    public ResponseEntity<DocumentoResponseDTO> buscarPorId(
            @Parameter(description = "ID del documento") @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                documentoService.buscarPorId(id)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar metadatos de un documento (HU-025)",
            description = "Solo se puede cambiar la categoria. El nombre y el archivo los fija la carga.")
    @ApiResponse(responseCode = "200", description = "Documento actualizado", content = @Content(schema = @Schema(implementation = DocumentoResponseDTO.class)))
    public ResponseEntity<DocumentoResponseDTO> actualizar(
            @Parameter(description = "ID del documento") @PathVariable Long id,
            @Valid @RequestBody DocumentoUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
                documentoService.actualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar documento (HU-026)",
            description = "Elimina el registro y sus fragmentos; el PDF se borra del storage solo si la transaccion confirma.")
    @ApiResponse(responseCode = "204", description = "Documento eliminado", content = @Content)
    @ApiResponse(responseCode = "409", description = "El documento esta en uso (sesiones o resumenes)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del documento") @PathVariable Long id
    ) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
