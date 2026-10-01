package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;

@RestController
@RequestMapping("/api")
@Tag(name = "Documents", description = "Documentos propios y preparacion para consultas")
public class SemanticSearchController {
    private final SemanticSearchService service;
    public SemanticSearchController(SemanticSearchService service) { this.service = service; }

    @PostMapping("/documents/{documentId}/embeddings")
    @Operation(summary = "Preparar documento para consultas", description = "Paso necesario tras generar fragmentos: el flujo actual no prepara automaticamente el documento. force=true permite reprocesarlo.")
    public EmbeddingGenerationResponseDTO generate(@PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean force) {
        return service.generar(documentId, force);
    }

}
