package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.DTOs.response.AuthResponseDTO;
import pe.edu.upc.legalai.DTOs.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.controllers.AuthController;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.servicesinterfaces.AuthService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private AuthResponseDTO sampleResponse() {
        UsuarioResponseDTO user = new UsuarioResponseDTO(1L, 2L, "ABOGADO", "Ana Torres",
                "ana@legalai.com", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());
        return new AuthResponseDTO("Bearer", "token-123", user);
    }

    @Test
    void registerCreaElUsuarioYDevuelve201() throws Exception {
        when(service.register(any())).thenReturn(sampleResponse());

        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"fullName":"Ana Torres","email":"ana@legalai.com","password":"password123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token-123"))
                .andExpect(jsonPath("$.user.email").value("ana@legalai.com"));
    }

    @Test
    void registerConCorreoDuplicadoDevuelve409() throws Exception {
        when(service.register(any())).thenThrow(new DuplicateResourceException("El correo ya esta registrado"));

        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"fullName":"Ana Torres","email":"ana@legalai.com","password":"password123"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void registerConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"fullName":"","email":"no-es-un-correo","password":"123"}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void loginConCredencialesValidasDevuelve200() throws Exception {
        when(service.login(any())).thenReturn(sampleResponse());

        mvc.perform(post("/api/auth/login").contentType("application/json").content("""
                        {"email":"ana@legalai.com","password":"password123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        when(service.login(any())).thenThrow(new UnauthorizedException("Credenciales invalidas"));

        mvc.perform(post("/api/auth/login").contentType("application/json").content("""
                        {"email":"ana@legalai.com","password":"incorrecta"}
                        """))
                .andExpect(status().isUnauthorized());
    }
}
