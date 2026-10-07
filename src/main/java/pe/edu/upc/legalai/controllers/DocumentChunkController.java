package pe.edu.upc.legalai.controllers;

import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.servicesinterfaces.DocumentChunkService;
import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/chunks")
public class DocumentChunkController {
    private final DocumentChunkService service;
    public DocumentChunkController(DocumentChunkService service) { this.service = service; }

    @PostMapping
    public ChunkGenerationResponseDTO generar(@PathVariable Long documentId) { return service.generar(documentId); }

    @GetMapping
    public List<DocumentChunkResponseDTO> listar(@PathVariable Long documentId) { return service.listar(documentId); }
}
