package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.ExpedienteController;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.IExpedienteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpedienteControllerTest {

    private IExpedienteService expedienteService;
    private IDocumentoService documentoService;
    private DocumentoPreparationService preparation;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        expedienteService = mock(IExpedienteService.class);
        documentoService = mock(IDocumentoService.class);
        preparation = mock(DocumentoPreparationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ExpedienteController(expedienteService, documentoService, preparation))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ExpedienteResponseDTO sampleExpediente(Long id, EstadoExpediente status, LocalDate openedAt) {
        return new ExpedienteResponseDTO(id, 1L, "Demanda laboral", "Descripcion",
                status, openedAt, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listarSinParametrosUsaElListadoDelUsuarioAutenticado() throws Exception {
        when(expedienteService.listarPorUsuarioAutenticado())
                .thenReturn(List.of(sampleExpediente(1L, EstadoExpediente.OPEN, LocalDate.now())));

        mvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarConEstadoYFechasDelegaEnElFiltroHU66() throws Exception {
        when(expedienteService.listarPorEstadoYFecha(eq(EstadoExpediente.CLOSED),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of(sampleExpediente(5L, EstadoExpediente.CLOSED, LocalDate.of(2026, 3, 10))));

        mvc.perform(get("/api/cases")
                        .param("status", "CLOSED")
                        .param("openedFrom", "2026-01-01")
                        .param("openedTo", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].caseId").value(5))
                .andExpect(jsonPath("$[0].status").value("CLOSED"));
    }

    @Test
    void listarConSoloEstadoDelegaEnElFiltroConFechasNulas() throws Exception {
        when(expedienteService.listarPorEstadoYFecha(eq(EstadoExpediente.OPEN), isNull(), isNull()))
                .thenReturn(List.of(sampleExpediente(2L, EstadoExpediente.OPEN, LocalDate.now())));

        mvc.perform(get("/api/cases").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarSinCoincidenciasDevuelve200YListaVaciaCA02() throws Exception {
        when(expedienteService.listarPorEstadoYFecha(eq(EstadoExpediente.ARCHIVED), isNull(), isNull()))
                .thenReturn(List.of());

        mvc.perform(get("/api/cases").param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarConEstadoInvalidoDevuelve400CA03() throws Exception {
        mvc.perform(get("/api/cases").param("status", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }
}
