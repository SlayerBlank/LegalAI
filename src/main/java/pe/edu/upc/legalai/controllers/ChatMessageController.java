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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.chatmessage.ChatMessageRequestDTO;
import pe.edu.upc.legalai.dtos.chatmessage.ChatMessageResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.services.interfaces.ChatMessageService;

import java.util.List;

@RestController
@RequestMapping("/api/chat-messages")
@Tag(name = "Mensajes de chat", description = "Operaciones relacionadas con los mensajes dentro de una sesión de chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @Operation(summary = "Registrar mensaje", description = "Registra un nuevo mensaje dentro de una sesión de chat")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensaje registrado correctamente",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sesión de chat no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ChatMessageResponseDTO> create(@Valid @RequestBody ChatMessageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.create(request));
    }

    @Operation(summary = "Listar mensajes", description = "Obtiene el listado completo de mensajes")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<ChatMessageResponseDTO>> findAll() {
        return ResponseEntity.ok(chatMessageService.findAll());
    }

    @Operation(summary = "Obtener mensaje por ID", description = "Busca un mensaje por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensaje encontrado",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ChatMessageResponseDTO> findById(
            @Parameter(description = "Identificador del mensaje", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(chatMessageService.findById(id));
    }

    @Operation(summary = "Listar mensajes por sesión",
            description = "Obtiene los mensajes de una sesión de chat específica, ordenados cronológicamente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Sesión de chat no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ChatMessageResponseDTO>> findBySession(
            @Parameter(description = "Identificador de la sesión de chat", example = "1") @PathVariable Long sessionId) {
        return ResponseEntity.ok(chatMessageService.findBySessionId(sessionId));
    }

    @Operation(summary = "Eliminar mensaje", description = "Elimina un mensaje por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensaje eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del mensaje", example = "1") @PathVariable Long id) {
        chatMessageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
