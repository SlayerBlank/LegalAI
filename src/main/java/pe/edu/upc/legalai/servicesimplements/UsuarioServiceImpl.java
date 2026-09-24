package pe.edu.upc.legalai.servicesimplements;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.UsuarioRepository;
import pe.edu.upc.legalai.DTOs.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .filter(usuario -> "ACTIVE".equals(usuario.getStatus()))
                .orElseThrow(() -> new UnauthorizedException("Usuario autenticado no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPerfilAutenticado() {
        return toResponse(obtenerUsuarioAutenticado());
    }

    public UsuarioResponseDTO toResponse(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getUserId(),
                usuario.getRol().getRoleId(),
                usuario.getRol().getName(),
                usuario.getFullName(),
                usuario.getEmail(),
                usuario.getStatus(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
