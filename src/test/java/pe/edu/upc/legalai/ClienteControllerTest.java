package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.ClienteController;
import pe.edu.upc.legalai.dtos.request.ClienteRequestDTO;
import pe.edu.upc.legalai.dtos.response.ClienteResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.entities.TipoCliente;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClienteControllerTest {

    private IExpedienteService expedienteService;
    private IClienteService clienteService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        clienteService = mock(IClienteService.class);
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

    // ---------- HU-012: registrar cliente

    @Test
    void registrarClienteValidoDevuelve201() throws Exception {
        when(clienteService.registrar(any(ClienteRequestDTO.class))).thenReturn(new ClienteResponseDTO(9L,
                TipoCliente.PERSON, "Ana Lopez", "12345678", "ana@mail.com", null, null,
                LocalDateTime.now(), LocalDateTime.now()));

        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientType\":\"PERSON\",\"fullNameOrCompany\":\"Ana Lopez\",\"documentNumber\":\"12345678\",\"email\":\"ana@mail.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(9))
                .andExpect(jsonPath("$.fullNameOrCompany").value("Ana Lopez"));
    }

    @Test
    void registrarClienteSinNombreDevuelve400ConMensajeComprensible() throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientType\":\"PERSON\",\"fullNameOrCompany\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fullNameOrCompany: El nombre o razon social es obligatorio"));
        verify(clienteService, never()).registrar(any());
    }

    @Test
    void registrarClienteConCorreoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientType\":\"COMPANY\",\"fullNameOrCompany\":\"ACME\",\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: El correo no tiene un formato valido"));
        verify(clienteService, never()).registrar(any());
    }

    @Test
    void registrarClienteConTipoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientType\":\"ALIEN\",\"fullNameOrCompany\":\"ACME\"}"))
                .andExpect(status().isBadRequest());
        verify(clienteService, never()).registrar(any());
    }
}
