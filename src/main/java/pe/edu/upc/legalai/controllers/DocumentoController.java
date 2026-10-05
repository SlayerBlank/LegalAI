package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.upc.legalai.dtos.response.DocumentoPreparationResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoTextResponseDTO;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoDownloadService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoProcessingService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(
        name = "Documents",
        description = "Consulta y eliminación de documentos propios"
)
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
    @Operation(
            summary = "Preparar o reintentar la preparación de un documento propio",
            description = "Reutiliza extracción y fragmentos existentes y genera los embeddings pendientes. "
                    + "Cada etapa confirma su propia transacción; un error no elimina el PDF original."
    )
    public DocumentoPreparationResponseDTO preparar(
            @PathVariable Long documentId
    ) {
        return preparation.preparar(documentId);
    }

    @GetMapping("/{documentId}/preparation")
    @Operation(
            summary = "Consultar si un documento propio está listo para RAG"
    )
    public DocumentoPreparationResponseDTO estadoPreparacion(
            @PathVariable Long documentId
    ) {
        return preparation.estado(documentId);
    }

    @GetMapping("/{documentId}/download")
    @Operation(
            summary = "Descargar un PDF propio",
            description = "Solo archivos provenientes de una carga verificable. No expone rutas del servidor."
    )
    public ResponseEntity<Resource> descargar(
            @PathVariable Long documentId
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

    @Operation(
            summary = "Extraer texto del PDF",
            description = "Procesa un PDF propio con texto seleccionable. No realiza OCR."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Texto extraído y documento procesado"
    )
    @ApiResponse(
            responseCode = "409",
            description = "El documento ya se está procesando"
    )
    @PostMapping("/{documentId}/process")
    public ResponseEntity<DocumentoResponseDTO> procesar(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                processingService.procesar(documentId)
        );
    }

    @Operation(
            summary = "Consultar texto extraído",
            description = "Devuelve el texto de un documento propio; text es null si no tiene extracción."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Texto del documento"
    )
    @GetMapping("/{documentId}/text")
    public ResponseEntity<DocumentoTextResponseDTO> obtenerTexto(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                processingService.obtenerTexto(documentId)
        );
    }

    @Operation(
            summary = "Listar documentos pendientes de revision por abogado",
            description = "Lista los documentos (titulo y expediente al que pertenecen) de todos los expedientes "
                    + "del abogado indicado que aun no completaron el procesamiento (UPLOADED, PROCESSING o ERROR) "
                    + "y por lo tanto estan pendientes de revision. Admite filtrar por un unico estado (HU-67)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido"
    )
    @ApiResponse(
            responseCode = "404",
            description = "El abogado indicado no existe"
    )
    @GetMapping("/pending-review/{abogadoId}")
    public ResponseEntity<List<DocumentoResponseDTO>> listarPendientesRevision(
            @PathVariable Long abogadoId,
            @Parameter(description = "Filtra por un estado de procesamiento especifico; "
                    + "por defecto incluye UPLOADED, PROCESSING y ERROR")
            @RequestParam(required = false) EstadoProcesamiento status
    ) {
        return ResponseEntity.ok(
                documentoService.listarPendientesRevision(abogadoId, status)
        );
    }

    @Operation(
            summary = "Obtener documento",
            description = "Obtiene metadatos de un documento propio"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Documento obtenido"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Documento no encontrado"
    )
    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                documentoService.buscarPorId(id)
        );
    }

    @Operation(
            summary = "Eliminar documento",
            description = "Elimina metadatos de un documento propio"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Documento eliminado"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}