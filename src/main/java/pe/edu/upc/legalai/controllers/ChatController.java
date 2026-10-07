package pe.edu.upc.legalai.controllers;

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
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponseDTO> crear(@Valid @RequestBody ChatCreateSessionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.crear(request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponseDTO>> listar(
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(chatService.listar(caseId, page, size));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponseDTO> obtener(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.obtener(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageResponseDTO> enviar(@PathVariable Long sessionId,
                                                          @Valid @RequestBody ChatSendMessageRequestDTO request) {
        return ResponseEntity.ok(chatService.enviarMensaje(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> historial(@PathVariable Long sessionId,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(chatService.historial(sessionId, page, size));
    }

    @PatchMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponseDTO> actualizar(@PathVariable Long sessionId,
                                                              @Valid @RequestBody ChatUpdateSessionRequestDTO request) {
        return ResponseEntity.ok(chatService.actualizarTitulo(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long sessionId) {
        chatService.eliminar(sessionId);
        return ResponseEntity.noContent().build();
    }
}