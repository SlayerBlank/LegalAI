package pe.edu.upc.legalai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.IUsuarioRepository;
import pe.edu.upc.legalai.servicesimplements.UsuarioServiceImplement;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceImplTest {

    private IUsuarioRepository usuarioRepository;
    private UsuarioServiceImplement service;

    @BeforeEach
    void setup() {
        usuarioRepository = mock(IUsuarioRepository.class);
        service = new UsuarioServiceImplement(usuarioRepository);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioActivo(String email) {
        Rol rol = new Rol();
        rol.setRoleId(2L);
        rol.setName("USER");
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);
        usuario.setRol(rol);
        usuario.setFullName("Juan Perez");
        usuario.setEmail(email);
        usuario.setStatus("ACTIVE");
        return usuario;
    }

    @Test
    void obtenerUsuarioAutenticadoSinAuthenticationLanzaUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> service.obtenerUsuarioAutenticado());
    }

    @Test
    void obtenerUsuarioAutenticadoNoAutenticadoLanzaUnauthorized() {
        var token = new TestingAuthenticationToken("juan@example.com", "x");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThrows(UnauthorizedException.class, () -> service.obtenerUsuarioAutenticado());
    }

    @Test
    void obtenerUsuarioAutenticadoAnonimoLanzaUnauthorized() {
        var anonimo = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonimo);

        assertThrows(UnauthorizedException.class, () -> service.obtenerUsuarioAutenticado());
    }

    @Test
    void obtenerUsuarioAutenticadoSinRegistroEnBdLanzaUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("fantasma@example.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> service.obtenerUsuarioAutenticado());
    }

    @Test
    void obtenerUsuarioAutenticadoConEstadoInactivoLanzaUnauthorized() {
        Usuario inactivo = usuarioActivo("juan@example.com");
        inactivo.setStatus("INACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan@example.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(inactivo));

        assertThrows(UnauthorizedException.class, () -> service.obtenerUsuarioAutenticado());
    }

    @Test
    void obtenerUsuarioAutenticadoActivoDevuelveElUsuario() {
        Usuario usuario = usuarioActivo("juan@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan@example.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));

        assertEquals(usuario, service.obtenerUsuarioAutenticado());
    }

    @Test
    void obtenerPerfilAutenticadoMapeaElUsuarioAlDto() {
        Usuario usuario = usuarioActivo("juan@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan@example.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));

        var response = service.obtenerPerfilAutenticado();

        assertEquals(1L, response.getUserId());
        assertEquals("USER", response.getRoleName());
        assertEquals("juan@example.com", response.getEmail());
        assertEquals("ACTIVE", response.getStatus());
    }
}
