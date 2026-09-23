package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.DTOs.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.DTOs.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.DTOs.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.ExpedienteService;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@Tag(name = "Expedientes", description = "Gestion de expedientes del usuario autenticado")
public class ExpedienteController {

    private final ExpedienteService expedienteService;
    private final DocumentoService documentoService;

    public ExpedienteController(ExpedienteService expedienteService, DocumentoService documentoService) {
        this.expedienteService = expedienteService;
        this.documentoService = documentoService;
    }

    @Operation(summary = "Crear expediente", description = "Crea un expediente sobre un cliente propio")
    @ApiResponse(responseCode = "201", description = "Expediente creado")
    @PostMapping
    public ResponseEntity<ExpedienteResponseDTO> registrar(@Valid @RequestBody ExpedienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expedienteService.registrar(request));
    }

    @Operation(summary = "Listar expedientes", description = "Lista expedientes del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ExpedienteResponseDTO>> listar() {
        return ResponseEntity.ok(expedienteService.listarPorUsuarioAutenticado());
    }

    @Operation(summary = "Obtener expediente", description = "Obtiene un expediente propio")
    @ApiResponse(responseCode = "200", description = "Expediente obtenido")
    @ApiResponse(responseCode = "404", description = "Expediente no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar expediente", description = "Actualiza un expediente propio")
    @ApiResponse(responseCode = "200", description = "Expediente actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDTO> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody ExpedienteRequestDTO request) {
        return ResponseEntity.ok(expedienteService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar expediente", description = "Elimina un expediente propio")
    @ApiResponse(responseCode = "204", description = "Expediente eliminado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        expedienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registrar documento", description = "Registra metadatos de documento en un expediente propio")
    @ApiResponse(responseCode = "201", description = "Documento registrado")
    @PostMapping("/{caseId}/documents")
    public ResponseEntity<DocumentoResponseDTO> registrarDocumento(@PathVariable Long caseId,
                                                                   @Valid @RequestBody DocumentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.registrar(caseId, request));
    }

    @Operation(summary = "Listar documentos de expediente", description = "Lista documentos de un expediente propio")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping("/{caseId}/documents")
    public ResponseEntity<List<DocumentoResponseDTO>> listarDocumentos(@PathVariable Long caseId) {
        return ResponseEntity.ok(documentoService.listarPorExpediente(caseId));
    }
}
