package pe.edu.upc.legalai.controllers;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upc.legalai.dtos.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.dtos.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteCantidadSesionesResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.IExpedienteService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cases")
public class ExpedienteController {

    private final IExpedienteService expedienteService;
    private final IDocumentoService documentoService;
    private final DocumentoPreparationService preparation;

    public ExpedienteController(IExpedienteService expedienteService, IDocumentoService documentoService,
                                DocumentoPreparationService preparation) {
        this.expedienteService = expedienteService;
        this.documentoService = documentoService;
        this.preparation = preparation;
    }

    @PostMapping
    public ResponseEntity<ExpedienteResponseDTO> registrar(@Valid @RequestBody ExpedienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expedienteService.registrar(request));
    }

    @GetMapping
    public ResponseEntity<List<ExpedienteResponseDTO>> listar(
            @RequestParam(required = false) EstadoExpediente status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedTo) {
        if (status == null && openedFrom == null && openedTo == null) {
            return ResponseEntity.ok(expedienteService.listarPorUsuarioAutenticado());
        }
        return ResponseEntity.ok(expedienteService.listarPorEstadoYFecha(status, openedFrom, openedTo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDTO> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody ExpedienteRequestDTO request) {
        return ResponseEntity.ok(expedienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        expedienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{caseId}/documents")
    public ResponseEntity<DocumentoResponseDTO> registrarDocumento(@PathVariable Long caseId,
                                                                   @Valid @RequestBody DocumentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.registrar(caseId, request));
    }

    @GetMapping("/{caseId}/documents")
    public ResponseEntity<List<DocumentoResponseDTO>> listarDocumentos(@PathVariable Long caseId) {
        return ResponseEntity.ok(documentoService.listarPorExpediente(caseId));
    }

    @PostMapping(value = "/{caseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> subirArchivo(
            @PathVariable Long caseId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "category", required = false) String category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preparation.subirYPreparar(caseId, file, category));
    }
    @GetMapping("/chat-session-count")
    public ResponseEntity<List<ExpedienteCantidadSesionesResponseDTO>> listarCantidadSesiones() {
        return ResponseEntity.ok(expedienteService.listarCantidadSesionesPorExpediente());
    }
}
