package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.controllers.AuditLogController;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLogControllerTest {

    private AuditLogService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(AuditLogService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuditLogController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private AuditLogResponseDTO sampleEntry(Long id, Long userId) {
        return new AuditLogResponseDTO(id, userId, "CREATE_CLIENT", "Cliente", 10L,
                "detalle", LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    void buscarSinFiltrosDevuelvePaginaCompleta() throws Exception {
        Page<AuditLogResponseDTO> page = new PageImpl<>(List.of(sampleEntry(1L, 5L), sampleEntry(2L, 6L)),
                PageRequest.of(0, 20), 2);
        when(service.buscar(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].logId").value(1))
                .andExpect(jsonPath("$.content[1].userId").value(6));
    }

    @Test
    void buscarConFiltrosLosPasaAlServicio() throws Exception {
        Page<AuditLogResponseDTO> page = new PageImpl<>(List.of(sampleEntry(3L, 5L)), PageRequest.of(0, 10), 1);
        when(service.buscar(eq(5L), eq("CREATE_CLIENT"), eq("Cliente"),
                eq(LocalDateTime.parse("2026-01-01T00:00:00")),
                eq(LocalDateTime.parse("2026-01-31T23:59:59")),
                any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/audit-logs")
                        .param("userId", "5")
                        .param("action", "CREATE_CLIENT")
                        .param("entityType", "Cliente")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-31T23:59:59")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].logId").value(3));

        verify(service).buscar(eq(5L), eq("CREATE_CLIENT"), eq("Cliente"),
                eq(LocalDateTime.parse("2026-01-01T00:00:00")),
                eq(LocalDateTime.parse("2026-01-31T23:59:59")),
                any(Pageable.class));
    }

    @Test
    void buscarConRangoDeFechasInvertidoDevuelve400() throws Exception {
        when(service.buscar(isNull(), isNull(), isNull(), any(), any(), any(Pageable.class)))
                .thenThrow(new BadRequestException("La fecha 'from' no puede ser posterior a 'to'"));

        mvc.perform(get("/api/audit-logs")
                        .param("from", "2026-02-01T00:00:00")
                        .param("to", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPorIdDevuelveElRegistroSolicitado() throws Exception {
        when(service.obtenerPorId(7L)).thenReturn(sampleEntry(7L, 3L));

        mvc.perform(get("/api/audit-logs/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logId").value(7))
                .andExpect(jsonPath("$.action").value("CREATE_CLIENT"));
    }

    @Test
    void obtenerPorIdInexistenteDevuelve404() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new ResourceNotFoundException("Registro de auditoria no encontrado con id: 99"));

        mvc.perform(get("/api/audit-logs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorUsuarioDevuelveSoloSusRegistros() throws Exception {
        when(service.listarPorUsuario(eq(5L))).thenReturn(List.of(sampleEntry(1L, 5L)));

        mvc.perform(get("/api/audit-logs/usuario/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(5));

        verify(service).listarPorUsuario(5L);
    }
}
