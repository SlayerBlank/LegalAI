package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.ClienteController;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.servicesinterfaces.IClienteService;
import pe.edu.upc.legalai.servicesinterfaces.IExpedienteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClienteControllerTest {

    private IExpedienteService expedienteService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        IClienteService clienteService = mock(IClienteService.class);
        expedienteService = mock(IExpedienteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService, expedienteService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void filtrarExpedientesDeClienteDelegaFiltrosHU068() throws Exception {
        LocalDate openedFrom = LocalDate.of(2026, 1, 1);
        LocalDate openedTo = LocalDate.of(2026, 10, 7);
        when(expedienteService.filtrarPorClienteEstadoYFechaApertura(
                1L, EstadoExpediente.OPEN, openedFrom, openedTo))
                .thenReturn(List.of(new ExpedienteResponseDTO(10L, 1L, "Caso", null,
                        EstadoExpediente.OPEN, openedFrom, null, LocalDateTime.now(), LocalDateTime.now())));

        mvc.perform(get("/api/clients/1/cases/filter")
                        .param("status", "OPEN")
                        .param("openedFrom", "2026-01-01")
                        .param("openedTo", "2026-10-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].caseId").value(10))
                .andExpect(jsonPath("$[0].openedAt").value("2026-01-01"));

        verify(expedienteService).filtrarPorClienteEstadoYFechaApertura(
                1L, EstadoExpediente.OPEN, openedFrom, openedTo);
    }

    @Test
    void listarExpedientesSinFiltroConservaElEndpointExistente() throws Exception {
        when(expedienteService.listarPorCliente(1L)).thenReturn(List.of());

        mvc.perform(get("/api/clients/1/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(expedienteService).listarPorCliente(1L);
    }
}
