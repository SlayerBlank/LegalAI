package pe.edu.upc.legalai.controllers;

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
public class ClienteController {

    private final IClienteService clienteService;
    private final IExpedienteService expedienteService;

    public ClienteController(IClienteService clienteService, IExpedienteService expedienteService) {
        this.clienteService = clienteService;
        this.expedienteService = expedienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrar(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.registrar(request));
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarPorUsuarioAutenticado());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{clientId}/cases")
    public ResponseEntity<List<ExpedienteResponseDTO>> listarExpedientes(@PathVariable Long clientId) {
        return ResponseEntity.ok(expedienteService.listarPorCliente(clientId));
    }

    // HU-068 - Expedientes por cliente, estado y rango de apertura.
    @GetMapping("/{clientId}/cases/filter")
    public ResponseEntity<List<ExpedienteResponseDTO>> filtrarExpedientes(
            @PathVariable Long clientId,
            @RequestParam EstadoExpediente status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedTo) {
        return ResponseEntity.ok(expedienteService.filtrarPorClienteEstadoYFechaApertura(
                clientId, status, openedFrom, openedTo));
    }

    @GetMapping("/document-count")
    public ResponseEntity<List<ClienteCantidadDocumentosResponseDTO>> listarCantidadDocumentos() {
        return ResponseEntity.ok(clienteService.listarCantidadDocumentosPorCliente());
    }
}
