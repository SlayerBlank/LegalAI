package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.DTOs.request.MensajeRequestDTO;
import pe.edu.upc.legalai.DTOs.response.MensajeResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.MensajesService;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Mensajes", description = "CRUD de mensajes de chat")
public class MensajesController {

    private final MensajesService mensajesService;

    public MensajesController(MensajesService mensajesService) {
        this.mensajesService = mensajesService;
    }

    @Operation(summary = "Crear mensaje")
    @ApiResponse(responseCode = "201", description = "Mensaje creado")
    @PostMapping
    public ResponseEntity<MensajeResponseDTO> crear(@Valid @RequestBody MensajeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensajesService.crear(request));
    }

    @Operation(summary = "Listar mensajes")
    @GetMapping
    public ResponseEntity<List<MensajeResponseDTO>> listar() {
        return ResponseEntity.ok(mensajesService.listar());
    }

    @Operation(summary = "Obtener mensaje")
    @GetMapping("/{id}")
    public ResponseEntity<MensajeResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mensajesService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar mensaje")
    @PutMapping("/{id}")
    public ResponseEntity<MensajeResponseDTO> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody MensajeRequestDTO request) {
        return ResponseEntity.ok(mensajesService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar mensaje")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mensajesService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
