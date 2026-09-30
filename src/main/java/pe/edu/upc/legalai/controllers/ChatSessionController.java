package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import pe.edu.upc.legalai.dtos.chatsession.ChatSessionRequestDTO;
import pe.edu.upc.legalai.dtos.chatsession.ChatSessionResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.ChatSessionService;

import java.util.List;

@RestController
@RequestMapping("/api/chat-sessions")
@Tag(name = "Sesiones de chat", description = "Operaciones relacionadas con la gestión de sesiones de chat")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @Operation(summary = "Crear sesión de chat", description = "Registra una nueva sesión de chat asociada a un expediente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesión creada correctamente",
                    content = @Content(schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Expediente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ChatSessionResponseDTO> create(@Valid @RequestBody ChatSessionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatSessionService.create(request));
    }

    @Operation(summary = "Listar sesiones de chat", description = "Obtiene el listado completo de sesiones de chat")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<ChatSessionResponseDTO>> findAll() {
        return ResponseEntity.ok(chatSessionService.findAll());
    }

    @Operation(summary = "Obtener sesión de chat por ID", description = "Busca una sesión de chat por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión encontrada",
                    content = @Content(schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ChatSessionResponseDTO> findById(
            @Parameter(description = "Identificador de la sesión", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(chatSessionService.findById(id));
    }

    @Operation(summary = "Listar sesiones por expediente",
            description = "Obtiene las sesiones de chat registradas para un expediente específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Expediente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<ChatSessionResponseDTO>> findByCase(
            @Parameter(description = "Identificador del expediente", example = "1") @PathVariable Long caseId) {
        return ResponseEntity.ok(chatSessionService.findByCaseId(caseId));
    }

    @Operation(summary = "Listar sesiones por usuario",
            description = "Obtiene las sesiones de chat registradas por un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatSessionResponseDTO>> findByUser(
            @Parameter(description = "Identificador del usuario", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(chatSessionService.findByUserId(userId));
    }

    @Operation(summary = "Actualizar sesión de chat", description = "Actualiza la información de una sesión existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión actualizada correctamente",
                    content = @Content(schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sesión, expediente o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ChatSessionResponseDTO> update(
            @Parameter(description = "Identificador de la sesión", example = "1") @PathVariable Long id,
            @Valid @RequestBody ChatSessionRequestDTO request) {
        return ResponseEntity.ok(chatSessionService.update(id, request));
    }

    @Operation(summary = "Eliminar sesión de chat", description = "Elimina una sesión de chat por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesión eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador de la sesión", example = "1") @PathVariable Long id) {
        chatSessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
