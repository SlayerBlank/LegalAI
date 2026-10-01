package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.dtos.response.ClienteResponseDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.controllers.ClienteController;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.TipoCliente;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.IClienteService;
import pe.edu.upc.legalai.servicesinterfaces.IExpedienteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClienteControllerTest {

    private IClienteService clienteService;
    private IExpedienteService expedienteService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        clienteService = mock(IClienteService.class);
        expedienteService = mock(IExpedienteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService, expedienteService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ClienteResponseDTO sampleCliente(Long id) {
        return new ClienteResponseDTO(id, TipoCliente.PERSON, "Carlos Lopez", "12345678",
                "carlos@email.com", "999999999", "Lima", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void registrarCreaElClienteYDevuelve201() throws Exception {
        when(clienteService.registrar(any())).thenReturn(sampleCliente(1L));

        mvc.perform(post("/api/clients").contentType("application/json").content("""
                        {"clientType":"PERSON","fullNameOrCompany":"Carlos Lopez","email":"carlos@email.com"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.fullNameOrCompany").value("Carlos Lopez"));
    }

    @Test
    void registrarSinNombreDevuelve400() throws Exception {
        mvc.perform(post("/api/clients").contentType("application/json").content("""
                        {"clientType":"PERSON"}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void listarDevuelveSoloClientesDelUsuarioAutenticado() throws Exception {
        when(clienteService.listarPorUsuarioAutenticado()).thenReturn(List.of(sampleCliente(1L), sampleCliente(2L)));

        mvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorIdExistenteDevuelveElCliente() throws Exception {
        when(clienteService.buscarPorId(5L)).thenReturn(sampleCliente(5L));

        mvc.perform(get("/api/clients/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(5));
    }

    @Test
    void buscarPorIdInexistenteDevuelve404() throws Exception {
        when(clienteService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Cliente no encontrado"));

        mvc.perform(get("/api/clients/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarDevuelveElClienteActualizado() throws Exception {
        when(clienteService.actualizar(eq(3L), any())).thenReturn(sampleCliente(3L));

        mvc.perform(put("/api/clients/3").contentType("application/json").content("""
                        {"clientType":"COMPANY","fullNameOrCompany":"Empresa SAC"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(3));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete("/api/clients/4"))
                .andExpect(status().isNoContent());

        verify(clienteService).eliminar(4L);
    }

    @Test
    void listarExpedientesDelClienteDevuelveElListado() throws Exception {
        ExpedienteResponseDTO expediente = new ExpedienteResponseDTO(10L, 1L, "Demanda laboral",
                "Descripcion", EstadoExpediente.OPEN, LocalDate.now(), null, LocalDateTime.now(), LocalDateTime.now());
        when(expedienteService.listarPorCliente(1L)).thenReturn(List.of(expediente));

        mvc.perform(get("/api/clients/1/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].caseId").value(10));
    }
}
