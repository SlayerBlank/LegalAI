package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Clients", description = "Gestion de clientes del usuario autenticado")
public class ClienteController {

    private final IClienteService clienteService;
    private final IExpedienteService expedienteService;

    public ClienteController(IClienteService clienteService, IExpedienteService expedienteService) {
        this.clienteService = clienteService;
        this.expedienteService = expedienteService;
    }

    @Operation(summary = "Crear cliente", description = "Registra un cliente para el usuario autenticado")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrar(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.registrar(request));
    }

    @Operation(summary = "Listar clientes", description = "Lista solo los clientes del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarPorUsuarioAutenticado());
    }

    @Operation(summary = "Obtener cliente", description = "Obtiene un cliente si pertenece al usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Cliente obtenido")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar cliente", description = "Actualiza un cliente propio")
    @ApiResponse(responseCode = "200", description = "Cliente actualizado")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente propio")
    @ApiResponse(responseCode = "204", description = "Cliente eliminado")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar expedientes por cliente", description = "Lista expedientes de un cliente propio")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @GetMapping("/{clientId}/cases")
    public ResponseEntity<List<ExpedienteResponseDTO>> listarExpedientes(@PathVariable Long clientId) {
        return ResponseEntity.ok(expedienteService.listarPorCliente(clientId));
    }

    // HU-068 - Query académica: expedientes por cliente, estado y rango de apertura
    @Operation(summary = "Filtrar expedientes de un cliente (HU-068)",
            description = "Filtra por estado y rango inclusivo de apertura; solo consulta clientes accesibles "
                    + "al usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Listado obtenido (puede ser vacio)")
    @ApiResponse(responseCode = "400", description = "Estado o rango de fechas invalido")
    @ApiResponse(responseCode = "401", description = "JWT ausente o invalido")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @GetMapping("/{clientId}/cases/filter")
    public ResponseEntity<List<ExpedienteResponseDTO>> filtrarExpedientes(
            @PathVariable Long clientId,
            @Parameter(description = "Estado del expediente", required = true)
            @RequestParam EstadoExpediente status,
            @Parameter(description = "Fecha de apertura desde (inclusive)", example = "2026-01-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedFrom,
            @Parameter(description = "Fecha de apertura hasta (inclusive)", example = "2026-10-07", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedTo) {
        return ResponseEntity.ok(expedienteService.filtrarPorClienteEstadoYFechaApertura(
                clientId, status, openedFrom, openedTo));
    }

    @Operation(summary = "Cantidad de documentos por cliente",
            description = "Lista el nombre de cada cliente del usuario autenticado y la cantidad de documentos asociados")
    @ApiResponse(responseCode = "200", description = "Listado obtenido (puede ser vacio)")
    @GetMapping("/document-count")
    public ResponseEntity<List<ClienteCantidadDocumentosResponseDTO>> listarCantidadDocumentos() {
        return ResponseEntity.ok(clienteService.listarCantidadDocumentosPorCliente());
    }
}
