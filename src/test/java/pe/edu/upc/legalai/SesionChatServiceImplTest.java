package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.request.ChatCreateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.ChatUpdateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.SesionChatRequestDTO;
import pe.edu.upc.legalai.dtos.response.ChatSessionResponseDTO;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesimplements.SesionChatServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.ChatService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class SesionChatServiceImplTest {

    private IUsuarioService usuarioService;
    private ChatService chatService;
    private SesionChatServiceImplement service;

    @BeforeEach
    void setup() {
        usuarioService = mock(IUsuarioService.class);
        chatService = mock(ChatService.class);
        service = new SesionChatServiceImplement(usuarioService, chatService);

        Usuario usuario = new Usuario();
        usuario.setUserId(7L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
    }

    private ChatSessionResponseDTO session(Long id, Long caseId, String title) {
        return new ChatSessionResponseDTO(id, caseId, null, title, 0L, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void registrarDelegaEnChatServiceYMapeaElUsuarioAutenticado() {
        when(chatService.crear(any())).thenReturn(session(1L, 5L, "Nueva conversacion"));
        SesionChatRequestDTO request = new SesionChatRequestDTO();
        request.setTitulo("Nueva conversacion");

        var response = service.registrar(5L, request);

        assertEquals(1L, response.getId());
        assertEquals(5L, response.getExpedienteId());
        assertEquals(7L, response.getUsuarioId());
        assertEquals("Nueva conversacion", response.getTitulo());

        var captor = org.mockito.ArgumentCaptor.forClass(ChatCreateSessionRequestDTO.class);
        verify(chatService).crear(captor.capture());
        assertEquals(5L, captor.getValue().getCaseId());
        assertEquals("Nueva conversacion", captor.getValue().getTitle());
    }

    @Test
    void listarPorExpedienteDelegaConElCaseIdYMapeaCadaSesion() {
        when(chatService.listar(eq(5L), isNull(), isNull())).thenReturn(List.of(session(1L, 5L, "A"), session(2L, 5L, "B")));

        var result = service.listarPorExpediente(5L);

        assertEquals(2, result.size());
        assertEquals(7L, result.get(0).getUsuarioId());
    }

    @Test
    void listarPorUsuarioAutenticadoDelegaSinFiltroDeExpediente() {
        when(chatService.listar(isNull(), isNull(), isNull())).thenReturn(List.of(session(3L, 9L, "C")));

        var result = service.listarPorUsuarioAutenticado();

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getExpedienteId());
    }

    @Test
    void buscarPorIdDelegaEnChatServiceObtener() {
        when(chatService.obtener(1L)).thenReturn(session(1L, 5L, "Titulo"));

        var response = service.buscarPorId(1L);

        assertEquals(1L, response.getId());
        verify(chatService).obtener(1L);
    }

    @Test
    void actualizarConTituloEnBlancoUsaElTituloPorDefecto() {
        when(chatService.actualizarTitulo(eq(1L), any())).thenReturn(session(1L, 5L, "Nueva conversacion"));
        SesionChatRequestDTO request = new SesionChatRequestDTO();
        request.setTitulo("   ");

        service.actualizar(1L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(ChatUpdateSessionRequestDTO.class);
        verify(chatService).actualizarTitulo(eq(1L), captor.capture());
        assertEquals("Nueva conversacion", captor.getValue().getTitle());
    }

    @Test
    void actualizarConTituloValidoLoRecortaYEnvia() {
        when(chatService.actualizarTitulo(eq(1L), any())).thenReturn(session(1L, 5L, "Titulo recortado"));
        SesionChatRequestDTO request = new SesionChatRequestDTO();
        request.setTitulo("  Titulo recortado  ");

        service.actualizar(1L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(ChatUpdateSessionRequestDTO.class);
        verify(chatService).actualizarTitulo(eq(1L), captor.capture());
        assertEquals("Titulo recortado", captor.getValue().getTitle());
    }

    @Test
    void eliminarDelegaDirectamenteEnChatService() {
        service.eliminar(1L);

        verify(chatService).eliminar(1L);
    }
}
