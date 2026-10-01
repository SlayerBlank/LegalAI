package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.dtos.response.SesionChatResponseDTO;
import pe.edu.upc.legalai.controllers.SesionChatController;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.ISesionChatService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SesionChatControllerTest {

    private ISesionChatService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(ISesionChatService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SesionChatController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private SesionChatResponseDTO sampleSesion(Long id) {
        SesionChatResponseDTO dto = new SesionChatResponseDTO();
        dto.setId(id);
        dto.setExpedienteId(1L);
        dto.setUsuarioId(2L);
        dto.setTitulo("Consulta sobre contrato");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    @Test
    void registrarCreaLaSesionYDevuelve201() throws Exception {
        when(service.registrar(eq(1L), any())).thenReturn(sampleSesion(1L));

        mvc.perform(post("/api/cases/1/chat-sessions").contentType("application/json").content("""
                        {"titulo":"Consulta sobre contrato"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Consulta sobre contrato"));
    }

    @Test
    void registrarSobreExpedienteInexistenteDevuelve404() throws Exception {
        when(service.registrar(eq(99L), any())).thenThrow(new ResourceNotFoundException("Expediente no encontrado"));

        mvc.perform(post("/api/cases/99/chat-sessions").contentType("application/json").content("""
                        {"titulo":"Consulta"}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorExpedienteDevuelveElListado() throws Exception {
        when(service.listarPorExpediente(1L)).thenReturn(List.of(sampleSesion(1L), sampleSesion(2L)));

        mvc.perform(get("/api/cases/1/chat-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listarDevuelveLasSesionesDelUsuarioAutenticado() throws Exception {
        when(service.listarPorUsuarioAutenticado()).thenReturn(List.of(sampleSesion(1L)));

        mvc.perform(get("/api/chat-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void buscarPorIdInexistenteDevuelve404() throws Exception {
        when(service.buscarPorId(50L)).thenThrow(new ResourceNotFoundException("Sesion no encontrada"));

        mvc.perform(get("/api/chat-sessions/50"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarDevuelveLaSesionActualizada() throws Exception {
        when(service.actualizar(eq(1L), any())).thenReturn(sampleSesion(1L));

        mvc.perform(put("/api/chat-sessions/1").contentType("application/json").content("""
                        {"titulo":"Titulo actualizado"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete("/api/chat-sessions/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }
}
