package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.request.ChatCreateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.ChatSendMessageRequestDTO;
import pe.edu.upc.legalai.dtos.request.ChatUpdateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.response.ChatMessageDTO;
import pe.edu.upc.legalai.dtos.response.ChatMessageResponseDTO;
import pe.edu.upc.legalai.dtos.response.ChatSessionResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat conversacional", description = "Conversaciones persistentes con memoria y RAG sobre expedientes propios")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Crear una sesion de chat",
            description = "El propietario se toma del JWT. Si documentId se omite la sesion usa todos los documentos del expediente")
    @ApiResponse(responseCode = "201", description = "Sesion creada")
    public ResponseEntity<ChatSessionResponseDTO> crear(@Valid @RequestBody ChatCreateSessionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.crear(request));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Listar mis sesiones de chat", description = "Ordenadas por updatedAt descendente")
    @ApiResponse(responseCode = "200", description = "Sesiones propias del usuario")
    public ResponseEntity<List<ChatSessionResponseDTO>> listar(
            @Parameter(description = "Filtro opcional por expediente propio") @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(chatService.listar(caseId, page, size));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Consultar una sesion propia", description = "No incluye el historial; use el endpoint de mensajes")
    @ApiResponse(responseCode = "200", description = "Sesion propia")
    public ResponseEntity<ChatSessionResponseDTO> obtener(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.obtener(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Enviar un mensaje y recibir la respuesta",
            description = "El alcance documental lo fija la sesion y el historial se limita a los ultimos mensajes configurados")
    @ApiResponse(responseCode = "200", description = "Turno completo con fuentes")
    @ApiResponse(responseCode = "503", description = "El modelo no genero respuesta; el mensaje del usuario permanece guardado")
    public ResponseEntity<ChatMessageResponseDTO> enviar(@PathVariable Long sessionId,
                                                          @Valid @RequestBody ChatSendMessageRequestDTO request) {
        return ResponseEntity.ok(chatService.enviarMensaje(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Recuperar el historial", description = "Ordenado por createdAt y messageId ascendentes")
    @ApiResponse(responseCode = "200", description = "Mensajes de la sesion propia")
    public ResponseEntity<List<ChatMessageDTO>> historial(@PathVariable Long sessionId,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(chatService.historial(sessionId, page, size));
    }

    @PatchMapping("/sessions/{sessionId}")
    @Operation(summary = "Cambiar el titulo de una sesion propia",
            description = "No permite cambiar propietario, expediente ni documento")
    @ApiResponse(responseCode = "200", description = "Sesion actualizada")
    public ResponseEntity<ChatSessionResponseDTO> actualizar(@PathVariable Long sessionId,
                                                              @Valid @RequestBody ChatUpdateSessionRequestDTO request) {
        return ResponseEntity.ok(chatService.actualizarTitulo(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Eliminar una sesion propia",
            description = "Elimina la sesion y sus mensajes; nunca documentos, chunks, embeddings ni archivos")
    @ApiResponse(responseCode = "204", description = "Sesion eliminada junto con sus mensajes")
    public ResponseEntity<Void> eliminar(@PathVariable Long sessionId) {
        chatService.eliminar(sessionId);
        return ResponseEntity.noContent().build();
    }
}