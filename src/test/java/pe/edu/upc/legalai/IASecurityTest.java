package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.legalai.controllers.IAController;
import pe.edu.upc.legalai.DTOs.response.IAResponseDTO;
import pe.edu.upc.legalai.securities.*;
import pe.edu.upc.legalai.servicesinterfaces.IAService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IAController.class, properties = "app.jwt.secret=test-only-secret-for-ai-security-tests-2026")
@Import({WebSecurityConfig.class, JwtRequestFilter.class, JwtAuthenticationEntryPoint.class, JwtTokenUtil.class})
class IASecurityTest {
    @Autowired private org.springframework.web.context.WebApplicationContext context;
    @Autowired private org.springframework.security.web.FilterChainProxy securityFilterChain;
    @Autowired private JwtTokenUtil tokens;
    @MockitoBean private IAService service;
    @MockitoBean private UsuarioDetailsService users;

    @Test
    void requiresValidBearerJwt() throws Exception {
        MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain).build();
        mvc.perform(post("/api/ai/test").contentType("application/json").content("{\"prompt\":\"Pregunta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ai/test").header("Authorization", "Bearer invalid")
                        .contentType("application/json").content("{\"prompt\":\"Pregunta\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        when(users.loadUserByUsername("ai@example.test"))
                .thenReturn(User.withUsername("ai@example.test").password("unused").roles("USER").build());
        when(service.generarRespuesta(any())).thenReturn(new IAResponseDTO());
        mvc.perform(post("/api/ai/test").header("Authorization", "Bearer " + tokens.generateToken("ai@example.test"))
                        .contentType("application/json").content("{\"prompt\":\"Pregunta\"}"))
                .andExpect(status().isOk());
        verify(service).generarRespuesta(any());
    }
}
