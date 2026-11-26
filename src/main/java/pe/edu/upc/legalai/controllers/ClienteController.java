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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.upc.legalai.dtos.request.ClienteRequestDTO;
import pe.edu.upc.legalai.dtos.response.ClienteCantidadDocumentosResponseDTO;
import pe.edu.upc.legalai.dtos.response.ClienteResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.servicesinterfaces.IClienteService;
import pe.edu.upc.legalai.servicesinterfaces.IExpedienteService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "cliente-controller", description = "Clientes del usuario autenticado. Un cliente ajeno responde 404.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Entrada invalida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT ausente, invalido o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Recurso inexistente o de otro usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class ClienteController {

    private final IClienteService clienteService;
    private final IExpedienteService expedienteService;

    public ClienteController(IClienteService clienteService, IExpedienteService expedienteService) {
        this.clienteService = clienteService;
        this.expedienteService = expedienteService;
    }

    @Operation(summary = "Registrar cliente (HU-012)", description = "Queda asociado al usuario autenticado. clientType: PERSON o COMPANY.")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrar(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.registrar(request));
    }

    @Operation(summary = "Listar mis clientes")
    @ApiResponse(responseCode = "200", description = "Clientes del usuario")
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarPorUsuarioAutenticado());
    }

    @Operation(summary = "Consultar cliente")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar cliente")
    @ApiResponse(responseCode = "200", description = "Cliente actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar cliente")
    @ApiResponse(responseCode = "204", description = "Cliente eliminado", content = @Content)
    @ApiResponse(responseCode = "409", description = "El cliente tiene expedientes asociados", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar expedientes de un cliente")
    @ApiResponse(responseCode = "200", description = "Expedientes del cliente")
    @GetMapping("/{clientId}/cases")
    public ResponseEntity<List<ExpedienteResponseDTO>> listarExpedientes(@PathVariable Long clientId) {
        return ResponseEntity.ok(expedienteService.listarPorCliente(clientId));
    }

    // HU-068 - Expedientes por cliente, estado y rango de apertura.
    @Operation(summary = "Filtrar expedientes de un cliente por estado y rango de apertura (HU-068)")
    @ApiResponse(responseCode = "200", description = "Expedientes que cumplen el filtro")
    @GetMapping("/{clientId}/cases/filter")
    public ResponseEntity<List<ExpedienteResponseDTO>> filtrarExpedientes(
            @PathVariable Long clientId,
            @RequestParam EstadoExpediente status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedTo) {
        return ResponseEntity.ok(expedienteService.filtrarPorClienteEstadoYFechaApertura(
                clientId, status, openedFrom, openedTo));
    }

    @Operation(summary = "Cantidad de documentos por cliente")
    @ApiResponse(responseCode = "200", description = "Conteo por cliente")
    @GetMapping("/document-count")
    public ResponseEntity<List<ClienteCantidadDocumentosResponseDTO>> listarCantidadDocumentos() {
        return ResponseEntity.ok(clienteService.listarCantidadDocumentosPorCliente());
    }
}
