package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upc.legalai.dtos.request.AuthLoginRequestDTO;
import pe.edu.upc.legalai.dtos.request.AuthRegisterRequestDTO;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.IRolRepository;
import pe.edu.upc.legalai.repositories.IUsuarioRepository;
import pe.edu.upc.legalai.securities.JwtTokenUtil;
import pe.edu.upc.legalai.servicesimplements.AuthServiceImpl;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private IUsuarioRepository usuarioRepository;
    private IRolRepository rolRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtTokenUtil jwtTokenUtil;
    private AuditLogService auditLogService;
    private AuthServiceImpl service;

    @BeforeEach
    void setup() {
        usuarioRepository = mock(IUsuarioRepository.class);
        rolRepository = mock(IRolRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        auditLogService = mock(AuditLogService.class);
        service = new AuthServiceImpl(usuarioRepository, rolRepository, passwordEncoder, authenticationManager,
                jwtTokenUtil, auditLogService);
    }

    private AuthRegisterRequestDTO registerRequest(String email, String password) {
        AuthRegisterRequestDTO request = new AuthRegisterRequestDTO();
        request.setFullName("Juan Perez");
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private Rol rolUsuario() {
        Rol rol = new Rol();
        rol.setRoleId(2L);
        rol.setName("USER");
        return rol;
    }

    @Test
    void registrarConEmailYaExistenteLanzaDuplicateResource() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.register(registerRequest("juan@example.com", "Secreta123")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarConContrasenaMuyLargaLanzaBadRequest() {
        String passwordLarga = "a".repeat(73);

        assertThrows(BadRequestException.class,
                () -> service.register(registerRequest("juan@example.com", passwordLarga)));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void registrarExitosoNormalizaEmailYDevuelveToken() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(rolRepository.findByName("USER")).thenReturn(Optional.of(rolUsuario()));
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setUserId(10L);
            return u;
        });
        when(jwtTokenUtil.generateToken("juan@example.com")).thenReturn("token-123");

        var response = service.register(registerRequest("JUAN@Example.com", "Secreta123"));

        assertEquals("token-123", response.getAccessToken());
        assertEquals("juan@example.com", response.getUser().getEmail());
    }

    @Test
    void loginConCredencialesInvalidasLanzaUnauthorized() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        AuthLoginRequestDTO request = new AuthLoginRequestDTO();
        request.setEmail("juan@example.com");
        request.setPassword("incorrecta");

        assertThrows(UnauthorizedException.class, () -> service.login(request));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void loginExitosoRegistraAuditoriaYDevuelveToken() {
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);
        usuario.setEmail("juan@example.com");
        usuario.setRol(rolUsuario());
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));
        when(jwtTokenUtil.generateToken("juan@example.com")).thenReturn("token-456");

        AuthLoginRequestDTO request = new AuthLoginRequestDTO();
        request.setEmail("juan@example.com");
        request.setPassword("Secreta123");

        var response = service.login(request);

        assertEquals("token-456", response.getAccessToken());
        verify(auditLogService).registrar(eq(usuario), eq("LOGIN"), eq("Usuario"), eq(1L), any());
    }
}
