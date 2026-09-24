package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.DTOs.request.CitacionIARequestDTO;
import pe.edu.upc.legalai.DTOs.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.CitacionesIAService;

import java.util.List;

@RestController
@RequestMapping("/api/citations")
@Tag(name = "Citaciones IA", description = "CRUD de citaciones generadas por IA")
public class CitacionesIAController {

    private final CitacionesIAService citacionesIAService;

    public CitacionesIAController(CitacionesIAService citacionesIAService) {
        this.citacionesIAService = citacionesIAService;
    }

    @Operation(summary = "Crear citación IA")
    @ApiResponse(responseCode = "201", description = "Citación creada")
    @PostMapping
    public ResponseEntity<CitacionIAResponseDTO> crear(@Valid @RequestBody CitacionIARequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citacionesIAService.crear(request));
    }

    @Operation(summary = "Listar citaciones")
    @GetMapping
    public ResponseEntity<List<CitacionIAResponseDTO>> listar() {
        return ResponseEntity.ok(citacionesIAService.listar());
    }

    @Operation(summary = "Listar citaciones de un mensaje")
    @GetMapping("/message/{messageId}")
    public ResponseEntity<List<CitacionIAResponseDTO>> listarPorMensaje(@PathVariable Long messageId) {
        return ResponseEntity.ok(citacionesIAService.listarPorMensaje(messageId));
    }

    @Operation(summary = "Obtener citación")
    @GetMapping("/{id}")
    public ResponseEntity<CitacionIAResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(citacionesIAService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar citación IA")
    @PutMapping("/{id}")
    public ResponseEntity<CitacionIAResponseDTO> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody CitacionIARequestDTO request) {
        return ResponseEntity.ok(citacionesIAService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar citación")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        citacionesIAService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
