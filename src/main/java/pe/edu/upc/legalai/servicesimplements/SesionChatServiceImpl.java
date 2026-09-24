package pe.edu.upc.legalai.servicesimplements;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.SesionChat;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.ExpedienteRepository;
import pe.edu.upc.legalai.repositories.SesionChatRepository;
import pe.edu.upc.legalai.repositories.UsuarioRepository;
import pe.edu.upc.legalai.schemas.dtos.request.SesionChatRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.SesionChatResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.SesionChatService;

import java.util.List;

@Service
public class SesionChatServiceImpl implements SesionChatService {

    private static final String TITULO_POR_DEFECTO = "Nueva conversacion";

    private final SesionChatRepository sesionChatRepository;
    private final ExpedienteRepository expedienteRepository;
    private final UsuarioRepository usuarioRepository;

    public SesionChatServiceImpl(SesionChatRepository sesionChatRepository,
                                 ExpedienteRepository expedienteRepository,
                                 UsuarioRepository usuarioRepository) {
        this.sesionChatRepository = sesionChatRepository;
        this.expedienteRepository = expedienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public SesionChatResponseDTO registrar(Long expedienteId, SesionChatRequestDTO request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        Expediente expediente = buscarExpedientePropio(expedienteId, usuario);

        SesionChat sesion = new SesionChat();
        sesion.setExpediente(expediente);
        sesion.setUsuario(usuario);
        sesion.setTitulo(tituloOPorDefecto(request.getTitulo()));

        return toResponse(sesionChatRepository.save(sesion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionChatResponseDTO> listarPorExpediente(Long expedienteId) {
        Usuario usuario = obtenerUsuarioAutenticado();
        buscarExpedientePropio(expedienteId, usuario);
        return sesionChatRepository.findByExpediente_CaseIdOrderByUpdatedAtDesc(expedienteId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionChatResponseDTO> listarPorUsuarioAutenticado() {
        Usuario usuario = obtenerUsuarioAutenticado();
        return sesionChatRepository.findByUsuario_UserIdOrderByUpdatedAtDesc(usuario.getUserId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SesionChatResponseDTO buscarPorId(Long id) {
        return toResponse(buscarSesionPropia(id, obtenerUsuarioAutenticado()));
    }

    @Override
    @Transactional
    public SesionChatResponseDTO actualizar(Long id, SesionChatRequestDTO request) {
        SesionChat sesion = buscarSesionPropia(id, obtenerUsuarioAutenticado());
        sesion.setTitulo(tituloOPorDefecto(request.getTitulo()));
        return toResponse(sesionChatRepository.save(sesion));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        SesionChat sesion = buscarSesionPropia(id, obtenerUsuarioAutenticado());
        sesionChatRepository.delete(sesion);
    }

    // ================= Metodos de apoyo =================

    private Usuario obtenerUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado"));
    }

    private Expediente buscarExpedientePropio(Long expedienteId, Usuario usuario) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con id: " + expedienteId));
        if (!expediente.getOwner().getUserId().equals(usuario.getUserId())) {
            throw new ResourceNotFoundException("Expediente no encontrado con id: " + expedienteId);
        }
        return expediente;
    }

    private SesionChat buscarSesionPropia(Long id, Usuario usuario) {
        SesionChat sesion = sesionChatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesion de chat no encontrada con id: " + id));
        if (!sesion.getUsuario().getUserId().equals(usuario.getUserId())) {
            throw new ResourceNotFoundException("Sesion de chat no encontrada con id: " + id);
        }
        return sesion;
    }

    private String tituloOPorDefecto(String titulo) {
        return (titulo == null || titulo.isBlank()) ? TITULO_POR_DEFECTO : titulo.trim();
    }

    private SesionChatResponseDTO toResponse(SesionChat sesion) {
        SesionChatResponseDTO dto = new SesionChatResponseDTO();
        dto.setId(sesion.getId());
        dto.setExpedienteId(sesion.getExpediente().getCaseId());
        dto.setUsuarioId(sesion.getUsuario().getUserId());
        dto.setTitulo(sesion.getTitulo());
        dto.setCreatedAt(sesion.getCreatedAt());
        dto.setUpdatedAt(sesion.getUpdatedAt());
        return dto;
    }
}