package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upc.legalai.dtos.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.dtos.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteAbiertoConAbogadoResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteCantidadSesionesResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.IExpedienteService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@Tag(name = "Cases", description = "Gestion de expedientes del usuario autenticado")
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

    @Operation(summary = "Cargar PDF", description = "Guarda el PDF e intenta preparar texto, fragmentos y embeddings. "
            + "La preparacion es sincrona; un fallo posterior a la carga conserva el archivo y devuelve sus metadatos. "
            + "Consulte /api/documents/{id}/preparation y reintente con /prepare. PROCESSED solo indica texto extraido.")
    @ApiResponse(responseCode = "201", description = "PDF guardado y documento creado")
    @ApiResponse(responseCode = "400", description = "Archivo, nombre o categoria invalidos")
    @ApiResponse(responseCode = "401", description = "JWT ausente o invalido")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @ApiResponse(responseCode = "404", description = "Expediente inexistente o ajeno")
    @ApiResponse(responseCode = "413", description = "Limite multipart excedido")
    @ApiResponse(responseCode = "500", description = "Error de almacenamiento o persistencia")
    @PostMapping(value = "/{caseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> subirArchivo(
            @PathVariable Long caseId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "category", required = false) String category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preparation.subirYPreparar(caseId, file, category));
    }
    // HU-063 - Query académica: casos abiertos con datos del abogado
    @Operation(summary = "Casos abiertos con datos del abogado",
            description = "Lista los expedientes OPEN del usuario autenticado y devuelve la información del abogado propietario asociado")
    @ApiResponse(responseCode = "200", description = "Listado obtenido (puede ser vacio)")
    @ApiResponse(responseCode = "401", description = "JWT ausente o invalido")
    @GetMapping("/open-with-lawyer")
    public ResponseEntity<List<ExpedienteAbiertoConAbogadoResponseDTO>> listarCasosAbiertosConAbogado() {
        return ResponseEntity.ok(expedienteService.listarCasosAbiertosConAbogado());
    }

    @Operation(summary = "Cantidad de sesiones de chat por expediente",
            description = "Lista el titulo de cada expediente del usuario autenticado y la cantidad de sesiones de chat asociadas")
    @ApiResponse(responseCode = "200", description = "Listado obtenido (puede ser vacio)")
    @GetMapping("/chat-session-count")
    public ResponseEntity<List<ExpedienteCantidadSesionesResponseDTO>> listarCantidadSesiones() {
        return ResponseEntity.ok(expedienteService.listarCantidadSesionesPorExpediente());
    }
}
