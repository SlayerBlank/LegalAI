package pe.edu.upc.legalai.controllers;

import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;

@RestController
@RequestMapping("/api")
public class SemanticSearchController {
    private final SemanticSearchService service;
    public SemanticSearchController(SemanticSearchService service) { this.service = service; }

    @PostMapping("/documents/{documentId}/embeddings")
    public EmbeddingGenerationResponseDTO generate(@PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean force) {
        return service.generar(documentId, force);
    }

}
