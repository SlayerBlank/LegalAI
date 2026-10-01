package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.dtos.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.controllers.UsuarioController;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest {

    private IUsuarioService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(IUsuarioService.class);
        mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service)).build();
    }

    @Test
    void meDevuelveElPerfilDelUsuarioAutenticado() throws Exception {
        UsuarioResponseDTO perfil = new UsuarioResponseDTO(1L, 2L, "ABOGADO", "Ana Torres",
                "ana@legalai.com", "ACTIVE", LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 8, 0));
        when(service.obtenerPerfilAutenticado()).thenReturn(perfil);

        mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.roleName").value("ABOGADO"))
                .andExpect(jsonPath("$.email").value("ana@legalai.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
