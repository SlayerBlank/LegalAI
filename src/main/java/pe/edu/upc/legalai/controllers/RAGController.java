package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.RAGRequestDTO;
import pe.edu.upc.legalai.dtos.response.RAGResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.RAGService;

@RestController
@RequestMapping("/api")
@Tag(name = "RAG", description = "Preguntas documentales sin historial; fuentes consultadas, no citas verificadas")
public class RAGController {
    private final RAGService service;
    public RAGController(RAGService service) { this.service = service; }
    @PostMapping("/documents/{documentId}/ask")
    @Operation(summary = "Responder usando fragmentos de un documento propio")
    public RAGResponseDTO document(@PathVariable Long documentId, @Valid @RequestBody RAGRequestDTO request) {
        return service.preguntarDocumento(documentId, request);
    }
    @PostMapping("/cases/{caseId}/ask")
    @Operation(summary = "Responder usando fragmentos del expediente propio")
    public RAGResponseDTO caseAsk(@PathVariable Long caseId, @Valid @RequestBody RAGRequestDTO request) {
        return service.preguntarExpediente(caseId, request);
    }
}
