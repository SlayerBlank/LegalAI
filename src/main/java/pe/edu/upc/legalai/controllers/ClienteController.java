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
import pe.edu.upc.legalai.schemas.dtos.request.ClienteRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.ClienteResponseDTO;
import pe.edu.upc.legalai.schemas.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ClienteService;
import pe.edu.upc.legalai.servicesinterfaces.ExpedienteService;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clientes", description = "Gestion de clientes del usuario autenticado")
public class ClienteController {

    private final ClienteService clienteService;
    private final ExpedienteService expedienteService;

    public ClienteController(ClienteService clienteService, ExpedienteService expedienteService) {
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
}
