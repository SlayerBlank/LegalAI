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
import pe.edu.upc.legalai.schemas.dtos.request.SesionChatRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.SesionChatResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.SesionChatService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Sesiones de chat", description = "Gestion de sesiones de chat sobre expedientes del usuario autenticado")
public class SesionChatController {

    private final SesionChatService sesionChatService;

    public SesionChatController(SesionChatService sesionChatService) {
        this.sesionChatService = sesionChatService;
    }

    @Operation(summary = "Crear sesion de chat", description = "Crea una sesion de chat en un expediente propio")
    @ApiResponse(responseCode = "201", description = "Sesion creada")
    @ApiResponse(responseCode = "404", description = "Expediente no encontrado")
    @PostMapping("/cases/{caseId}/chat-sessions")
    public ResponseEntity<SesionChatResponseDTO> registrar(@PathVariable Long caseId,
                                                           @Valid @RequestBody SesionChatRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sesionChatService.registrar(caseId, request));
    }

    @Operation(summary = "Listar sesiones de expediente", description = "Lista sesiones de chat de un expediente propio")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping("/cases/{caseId}/chat-sessions")
    public ResponseEntity<List<SesionChatResponseDTO>> listarPorExpediente(@PathVariable Long caseId) {
        return ResponseEntity.ok(sesionChatService.listarPorExpediente(caseId));
    }

    @Operation(summary = "Listar mis sesiones", description = "Lista sesiones de chat del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping("/chat-sessions")
    public ResponseEntity<List<SesionChatResponseDTO>> listar() {
        return ResponseEntity.ok(sesionChatService.listarPorUsuarioAutenticado());
    }

    @Operation(summary = "Obtener sesion de chat", description = "Obtiene una sesion de chat propia")
    @ApiResponse(responseCode = "200", description = "Sesion obtenida")
    @ApiResponse(responseCode = "404", description = "Sesion no encontrada")
    @GetMapping("/chat-sessions/{id}")
    public ResponseEntity<SesionChatResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sesionChatService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar sesion de chat", description = "Actualiza el titulo de una sesion propia")
    @ApiResponse(responseCode = "200", description = "Sesion actualizada")
    @PutMapping("/chat-sessions/{id}")
    public ResponseEntity<SesionChatResponseDTO> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody SesionChatRequestDTO request) {
        return ResponseEntity.ok(sesionChatService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar sesion de chat", description = "Elimina una sesion propia")
    @ApiResponse(responseCode = "204", description = "Sesion eliminada")
    @DeleteMapping("/chat-sessions/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        sesionChatService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}