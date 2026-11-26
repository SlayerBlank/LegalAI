package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
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
@Tag(name = "expediente-controller", description = "Expedientes del usuario autenticado. Un expediente ajeno responde 404.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Entrada invalida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT ausente, invalido o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Recurso inexistente o de otro usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
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

    @Operation(summary = "Registrar expediente")
    @ApiResponse(responseCode = "201", description = "Expediente creado")
    @PostMapping
    public ResponseEntity<ExpedienteResponseDTO> registrar(@Valid @RequestBody ExpedienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expedienteService.registrar(request));
    }

    @Operation(summary = "Listar expedientes, opcionalmente por estado y rango de apertura (HU-66)", description = "Sin filtros lista todos. openedFrom y openedTo (yyyy-MM-dd) son inclusivos; openedFrom posterior a openedTo responde 400.")
    @ApiResponse(responseCode = "200", description = "Expedientes del usuario (lista vacia si no hay coincidencias)")
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

    @Operation(summary = "Consultar expediente")
    @ApiResponse(responseCode = "200", description = "Expediente encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar expediente")
    @ApiResponse(responseCode = "200", description = "Expediente actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDTO> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody ExpedienteRequestDTO request) {
        return ResponseEntity.ok(expedienteService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar expediente (HU-021)")
    @ApiResponse(responseCode = "204", description = "Expediente eliminado", content = @Content)
    @ApiResponse(responseCode = "409", description = "El expediente tiene documentos, borradores, sesiones o resumenes asociados", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        expedienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registrar metadatos de un documento")
    @ApiResponse(responseCode = "201", description = "Documento registrado")
    @PostMapping("/{caseId}/documents")
    public ResponseEntity<DocumentoResponseDTO> registrarDocumento(@PathVariable Long caseId,
                                                                   @Valid @RequestBody DocumentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.registrar(caseId, request));
    }

    @Operation(summary = "Listar documentos de un expediente")
    @ApiResponse(responseCode = "200", description = "Documentos del expediente")
    @GetMapping("/{caseId}/documents")
    public ResponseEntity<List<DocumentoResponseDTO>> listarDocumentos(@PathVariable Long caseId) {
        return ResponseEntity.ok(documentoService.listarPorExpediente(caseId));
    }

    @Operation(summary = "Cargar PDF a un expediente (HU-022)", description = "multipart/form-data con file (PDF, maximo 10 MB) y category opcional (maximo 80). Luego intenta prepararlo para IA.")
    @ApiResponse(responseCode = "201", description = "Documento cargado")
    @ApiResponse(responseCode = "413", description = "El archivo supera el tamano maximo")
    @PostMapping(value = "/{caseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> subirArchivo(
            @PathVariable Long caseId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "category", required = false) String category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preparation.subirYPreparar(caseId, file, category));
    }
    @Operation(summary = "Cantidad de sesiones de chat por expediente")
    @ApiResponse(responseCode = "200", description = "Conteo por expediente")
    @GetMapping("/chat-session-count")
    public ResponseEntity<List<ExpedienteCantidadSesionesResponseDTO>> listarCantidadSesiones() {
        return ResponseEntity.ok(expedienteService.listarCantidadSesionesPorExpediente());
    }
}
