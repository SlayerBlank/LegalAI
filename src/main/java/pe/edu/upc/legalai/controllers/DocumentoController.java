package pe.edu.upc.legalai.controllers;

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
    public DocumentoPreparationResponseDTO preparar(
            @PathVariable Long documentId
    ) {
        return preparation.preparar(documentId);
    }

    @GetMapping("/{documentId}/preparation")
    public DocumentoPreparationResponseDTO estadoPreparacion(
            @PathVariable Long documentId
    ) {
        return preparation.estado(documentId);
    }

    @GetMapping("/{documentId}/download")
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

    @PostMapping("/{documentId}/process")
    public ResponseEntity<DocumentoResponseDTO> procesar(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                processingService.procesar(documentId)
        );
    }

    @GetMapping("/{documentId}/text")
    public ResponseEntity<DocumentoTextResponseDTO> obtenerTexto(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(
                processingService.obtenerTexto(documentId)
        );
    }

    @GetMapping("/pending-review/{abogadoId}")
    public ResponseEntity<List<DocumentoResponseDTO>> listarPendientesRevision(
            @PathVariable Long abogadoId,
            @RequestParam(required = false) EstadoProcesamiento status
    ) {
        return ResponseEntity.ok(
                documentoService.listarPendientesRevision(abogadoId, status)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                documentoService.buscarPorId(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}