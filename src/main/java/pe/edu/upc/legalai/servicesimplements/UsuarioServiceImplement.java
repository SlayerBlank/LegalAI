package pe.edu.upc.legalai.servicesimplements;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.IUsuarioRepository;
import pe.edu.upc.legalai.dtos.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;
import pe.edu.upc.legalai.dtos.request.UsuarioUpdateRequestDTO;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioServiceImplement implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;

    public UsuarioServiceImplement(IUsuarioRepository usuarioRepository) {
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

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll(Sort.by("userId")).stream().map(this::toResponse).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return toResponse(getUsuario(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioUpdateRequestDTO request) {
        Usuario usuario = getUsuario(id, true);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCaseAndUserIdNot(email, id)) {
            throw new DuplicateResourceException("El correo ya esta registrado");
        }
        // JWT sub is the email. Reassigning it could authenticate an old token as another account.
        if (!usuario.getEmail().equals(email)) {
            throw new BadRequestException("El correo es la identidad JWT y no puede modificarse");
        }
        usuario.setFullName(request.fullName().trim());
        return toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = getUsuario(id, true);
        if ("ADMIN".equalsIgnoreCase(usuario.getRol().getName())
                || usuario.getUserId().equals(obtenerUsuarioAutenticado().getUserId())) {
            throw new DuplicateResourceException("No se permite dar de baja administradores ni la cuenta autenticada");
        }
        // Preserve FK history and reserve the email against reuse by an outstanding JWT.
        usuario.setStatus("INACTIVE");
        usuarioRepository.saveAndFlush(usuario);
    }

    private Usuario getUsuario(Long id) {
        return getUsuario(id, false);
    }

    private Usuario getUsuario(Long id, boolean forUpdate) {
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del usuario debe ser positivo");
        }
        return (forUpdate ? usuarioRepository.findByIdForUpdate(id) : usuarioRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
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
