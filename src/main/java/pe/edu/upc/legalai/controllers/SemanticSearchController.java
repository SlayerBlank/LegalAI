package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.SemanticSearchRequestDTO;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;

import java.util.List;

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

    // HU-101: búsqueda semántica dentro de un documento propio.
    @PostMapping("/documents/{documentId}/search")
    @Operation(summary = "Busqueda semantica en un documento",
            description = "Devuelve los fragmentos mas cercanos a la consulta (topK entre 1 y 20), ordenados por distancia")
    @ApiResponse(responseCode = "200", description = "Fragmentos encontrados (puede ser vacio)")
    @ApiResponse(responseCode = "400", description = "query vacia o topK fuera de rango")
    @ApiResponse(responseCode = "404", description = "Documento ajeno o inexistente")
    public List<SemanticSearchResultDTO> searchDocument(@PathVariable Long documentId,
            @Valid @RequestBody SemanticSearchRequestDTO request) {
        return service.searchDocument(documentId, request);
    }

    // HU-101: búsqueda semántica en todos los documentos de un expediente propio.
    @PostMapping("/cases/{caseId}/search")
    @Operation(summary = "Busqueda semantica en un expediente",
            description = "Busca en los fragmentos de todos los documentos del expediente (topK entre 1 y 20)")
    @ApiResponse(responseCode = "200", description = "Fragmentos encontrados (puede ser vacio)")
    @ApiResponse(responseCode = "400", description = "query vacia o topK fuera de rango")
    @ApiResponse(responseCode = "404", description = "Expediente ajeno o inexistente")
    public List<SemanticSearchResultDTO> searchCase(@PathVariable Long caseId,
            @Valid @RequestBody SemanticSearchRequestDTO request) {
        return service.searchCase(caseId, request);
    }
}
