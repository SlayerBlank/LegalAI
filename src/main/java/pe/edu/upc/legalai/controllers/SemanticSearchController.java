package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.SemanticSearchRequestDTO;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Busqueda semantica", description = "Embeddings y retrieval de documentos propios")
public class SemanticSearchController {
    private final SemanticSearchService service;
    public SemanticSearchController(SemanticSearchService service) { this.service = service; }

    @PostMapping("/documents/{documentId}/embeddings")
    @Operation(summary = "Generar embeddings; force=true regenera todos de forma atomica")
    public EmbeddingGenerationResponseDTO generate(@PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean force) {
        return service.generar(documentId, force);
    }

    @PostMapping("/documents/{documentId}/search")
    @Operation(summary = "Buscar los chunks mas cercanos de un documento; menor distance es mejor")
    public List<SemanticSearchResultDTO> document(@PathVariable Long documentId,
            @Valid @RequestBody SemanticSearchRequestDTO request) {
        return service.searchDocument(documentId, request);
    }

    @PostMapping("/cases/{caseId}/search")
    @Operation(summary = "Buscar entre todos los documentos de un expediente propio")
    public List<SemanticSearchResultDTO> caseSearch(@PathVariable Long caseId,
            @Valid @RequestBody SemanticSearchRequestDTO request) {
        return service.searchCase(caseId, request);
    }
}
