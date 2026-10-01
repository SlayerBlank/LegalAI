package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.dtos.response.RolResponseDTO;
import pe.edu.upc.legalai.controllers.RolController;
import pe.edu.upc.legalai.servicesinterfaces.IRolService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolControllerTest {

    private IRolService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(IRolService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RolController(service)).build();
    }

    @Test
    void listarDevuelveLosRolesDisponibles() throws Exception {
        when(service.listar()).thenReturn(List.of(
                new RolResponseDTO(1L, "ADMIN", "Administrador de la plataforma"),
                new RolResponseDTO(2L, "ABOGADO", "Profesional del derecho")
        ));

        mvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("ADMIN"))
                .andExpect(jsonPath("$[1].name").value("ABOGADO"));
    }

    @Test
    void listarSinRolesDevuelveListaVacia() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
