package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.schemas.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoService;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documentos", description = "Consulta y eliminacion de documentos propios")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Operation(summary = "Obtener documento", description = "Obtiene metadatos de un documento propio")
    @ApiResponse(responseCode = "200", description = "Documento obtenido")
    @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }

    @Operation(summary = "Eliminar documento", description = "Elimina metadatos de un documento propio")
    @ApiResponse(responseCode = "204", description = "Documento eliminado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
