package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Cliente;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.repositories.ClienteRepository;
import pe.edu.upc.legalai.repositories.ExpedienteRepository;
import pe.edu.upc.legalai.schemas.dtos.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.ExpedienteService;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExpedienteServiceImpl implements ExpedienteService {

    private final ExpedienteRepository expedienteRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;
    private final AuditLogService auditLogService;

    public ExpedienteServiceImpl(ExpedienteRepository expedienteRepository, ClienteRepository clienteRepository,
                                 UsuarioService usuarioService, AuditLogService auditLogService) {
        this.expedienteRepository = expedienteRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO registrar(ExpedienteRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Cliente cliente = getCliente(request.getClientId(), usuario.getUserId());
        Expediente expediente = new Expediente();
        expediente.setOwner(usuario);
        expediente.setClient(cliente);
        applyRequest(expediente, request);
        Expediente saved = expedienteRepository.save(expediente);
        auditLogService.registrar(usuario, "CREATE_CASE", "Expediente", saved.getCaseId(), saved.getTitle());
        if (saved.getStatus() == EstadoExpediente.CLOSED) {
            auditLogService.registrar(usuario, "CLOSE_CASE", "Expediente", saved.getCaseId(), saved.getTitle());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorUsuarioAutenticado() {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return expedienteRepository.findByOwnerUserId(usuario.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorCliente(Long clientId) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        getCliente(clientId, usuario.getUserId());
        return expedienteRepository.findByClientClientIdAndOwnerUserId(clientId, usuario.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return toResponse(getExpediente(id, usuario.getUserId()));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO actualizar(Long id, ExpedienteRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Expediente expediente = getExpediente(id, usuario.getUserId());
        Cliente cliente = getCliente(request.getClientId(), usuario.getUserId());
        EstadoExpediente previousStatus = expediente.getStatus();
        expediente.setClient(cliente);
        applyRequest(expediente, request);
        Expediente saved = expedienteRepository.saveAndFlush(expediente);
        auditLogService.registrar(usuario, "UPDATE_CASE", "Expediente", saved.getCaseId(), saved.getTitle());
        if (previousStatus != EstadoExpediente.CLOSED && saved.getStatus() == EstadoExpediente.CLOSED) {
            auditLogService.registrar(usuario, "CLOSE_CASE", "Expediente", saved.getCaseId(), saved.getTitle());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Expediente expediente = getExpediente(id, usuario.getUserId());
        expedienteRepository.delete(expediente);
    }

    private Cliente getCliente(Long clientId, Long userId) {
        return clienteRepository.findByClientIdAndOwnerUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private Expediente getExpediente(Long caseId, Long userId) {
        return expedienteRepository.findByCaseIdAndOwnerUserId(caseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado"));
    }

    private void applyRequest(Expediente expediente, ExpedienteRequestDTO request) {
        EstadoExpediente status = request.getStatus() == null ? expediente.getStatus() : request.getStatus();
        LocalDate openedAt = request.getOpenedAt() == null ? expediente.getOpenedAt() : request.getOpenedAt();
        if (openedAt == null) {
            openedAt = LocalDate.now();
        }
        LocalDate closedAt = request.getClosedAt();
        if (status == EstadoExpediente.OPEN || status == EstadoExpediente.IN_PROGRESS) {
            if (closedAt != null) {
                throw new BadRequestException("Un expediente abierto no puede tener fecha de cierre");
            }
        } else if (closedAt == null) {
            closedAt = expediente.getClosedAt();
            if (closedAt == null && status == EstadoExpediente.CLOSED) {
                closedAt = LocalDate.now();
            }
        }
        if (closedAt != null && closedAt.isBefore(openedAt)) {
            throw new BadRequestException("La fecha de cierre no puede ser anterior a la fecha de apertura");
        }
        expediente.setTitle(request.getTitle());
        expediente.setDescription(request.getDescription());
        expediente.setStatus(status);
        expediente.setOpenedAt(openedAt);
        expediente.setClosedAt(closedAt);
    }

    private ExpedienteResponseDTO toResponse(Expediente expediente) {
        return new ExpedienteResponseDTO(
                expediente.getCaseId(),
                expediente.getClient().getClientId(),
                expediente.getTitle(),
                expediente.getDescription(),
                expediente.getStatus(),
                expediente.getOpenedAt(),
                expediente.getClosedAt(),
                expediente.getCreatedAt(),
                expediente.getUpdatedAt()
        );
    }
}
