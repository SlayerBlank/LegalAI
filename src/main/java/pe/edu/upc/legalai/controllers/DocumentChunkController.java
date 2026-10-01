package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.servicesinterfaces.DocumentChunkService;
import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/chunks")
@Tag(name = "Documents", description = "Documentos propios y sus fragmentos")
public class DocumentChunkController {
    private final DocumentChunkService service;
    public DocumentChunkController(DocumentChunkService service) { this.service = service; }

    @Operation(summary = "Generar chunks", description = "Reemplaza atomicamente los chunks de un documento propio PROCESSED; no modifica su estado.")
    @ApiResponse(responseCode = "200", description = "Chunks generados")
    @PostMapping
    public ChunkGenerationResponseDTO generar(@PathVariable Long documentId) { return service.generar(documentId); }

    @Operation(summary = "Listar chunks", description = "Fragmentos del documento propio ordenados por chunkIndex. Offsets UTF-16 del texto normalizado, fin exclusivo.")
    @ApiResponse(responseCode = "200", description = "Chunks ordenados")
    @GetMapping
    public List<DocumentChunkResponseDTO> listar(@PathVariable Long documentId) { return service.listar(documentId); }
}
