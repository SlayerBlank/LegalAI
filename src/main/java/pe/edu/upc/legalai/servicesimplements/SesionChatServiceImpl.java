package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import pe.edu.upc.legalai.dtos.request.ChatCreateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.ChatUpdateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.SesionChatRequestDTO;
import pe.edu.upc.legalai.dtos.response.ChatSessionResponseDTO;
import pe.edu.upc.legalai.dtos.response.SesionChatResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ChatService;
import pe.edu.upc.legalai.servicesinterfaces.SesionChatService;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

import java.util.List;

/**
 * Fachada de compatibilidad de /api/chat-sessions. Delega la logica de negocio en
 * ChatService para no duplicar validaciones de propiedad, borrado en cascada ni auditoria.
 */
@Service
public class SesionChatServiceImpl implements SesionChatService {

    private static final String TITULO_POR_DEFECTO = "Nueva conversacion";

    private final UsuarioService usuarioService;
    private final ChatService chatService;

    public SesionChatServiceImpl(UsuarioService usuarioService, ChatService chatService) {
        this.usuarioService = usuarioService;
        this.chatService = chatService;
    }

    @Override
    public SesionChatResponseDTO registrar(Long expedienteId, SesionChatRequestDTO request) {
        Long userId = usuarioService.obtenerUsuarioAutenticado().getUserId();
        ChatCreateSessionRequestDTO body = new ChatCreateSessionRequestDTO();
        body.setCaseId(expedienteId);
        body.setTitle(request.getTitulo());
        return toLegacy(chatService.crear(body), userId);
    }

    @Override
    public List<SesionChatResponseDTO> listarPorExpediente(Long expedienteId) {
        Long userId = usuarioService.obtenerUsuarioAutenticado().getUserId();
        return chatService.listar(expedienteId, null, null).stream().map(session -> toLegacy(session, userId)).toList();
    }

    @Override
    public List<SesionChatResponseDTO> listarPorUsuarioAutenticado() {
        Long userId = usuarioService.obtenerUsuarioAutenticado().getUserId();
        return chatService.listar(null, null, null).stream().map(session -> toLegacy(session, userId)).toList();
    }

    @Override
    public SesionChatResponseDTO buscarPorId(Long id) {
        Long userId = usuarioService.obtenerUsuarioAutenticado().getUserId();
        return toLegacy(chatService.obtener(id), userId);
    }

    @Override
    public SesionChatResponseDTO actualizar(Long id, SesionChatRequestDTO request) {
        Long userId = usuarioService.obtenerUsuarioAutenticado().getUserId();
        ChatUpdateSessionRequestDTO body = new ChatUpdateSessionRequestDTO();
        body.setTitle(tituloOporDefecto(request.getTitulo()));
        return toLegacy(chatService.actualizarTitulo(id, body), userId);
    }

    @Override
    public void eliminar(Long id) {
        chatService.eliminar(id);
    }

    private static SesionChatResponseDTO toLegacy(ChatSessionResponseDTO session, Long userId) {
        SesionChatResponseDTO dto = new SesionChatResponseDTO();
        dto.setId(session.sessionId());
        dto.setExpedienteId(session.caseId());
        dto.setUsuarioId(userId);
        dto.setTitulo(session.title());
        dto.setCreatedAt(session.createdAt());
        dto.setUpdatedAt(session.updatedAt());
        return dto;
    }

    private static String tituloOporDefecto(String titulo) {
        return titulo == null || titulo.isBlank() ? TITULO_POR_DEFECTO : titulo.trim();
    }
}