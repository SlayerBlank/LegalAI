package pe.edu.upc.legalai.servicesimplements;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.RolRepository;
import pe.edu.upc.legalai.repositories.UsuarioRepository;
import pe.edu.upc.legalai.DTOs.request.AuthLoginRequestDTO;
import pe.edu.upc.legalai.DTOs.request.AuthRegisterRequestDTO;
import pe.edu.upc.legalai.DTOs.response.AuthResponseDTO;
import pe.edu.upc.legalai.DTOs.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.securities.JwtTokenUtil;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.AuthService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtTokenUtil jwtTokenUtil, AuditLogService auditLogService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AuthResponseDTO register(AuthRegisterRequestDTO request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("La contrasena no debe superar los 72 bytes UTF-8");
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("El correo ya esta registrado");
        }
        Rol rol = rolRepository.findByName("USER").orElseGet(() -> crearRolUsuario());
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setFullName(request.getFullName());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setStatus("ACTIVE");
        Usuario saved = usuarioRepository.save(usuario);
        String token = jwtTokenUtil.generateToken(saved.getEmail());
        return new AuthResponseDTO("Bearer", token, toUsuarioResponse(saved));
    }

    @Override
    @Transactional
    public AuthResponseDTO login(AuthLoginRequestDTO request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getEmail().trim().toLowerCase(Locale.ROOT),
                    request.getPassword()
            ));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Credenciales invalidas");
        }
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));
        auditLogService.registrar(usuario, "LOGIN", "Usuario", usuario.getUserId(), "Inicio de sesion exitoso");
        String token = jwtTokenUtil.generateToken(usuario.getEmail());
        return new AuthResponseDTO("Bearer", token, toUsuarioResponse(usuario));
    }

    private Rol crearRolUsuario() {
        rolRepository.crearRolUsuarioSiNoExiste();
        return rolRepository.findByName("USER").orElseThrow();
    }

    private UsuarioResponseDTO toUsuarioResponse(Usuario usuario) {
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
