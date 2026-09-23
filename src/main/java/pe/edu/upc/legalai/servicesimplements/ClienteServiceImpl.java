package pe.edu.upc.legalai.servicesimplements;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Cliente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.ClienteRepository;
import pe.edu.upc.legalai.DTOs.request.ClienteRequestDTO;
import pe.edu.upc.legalai.DTOs.response.ClienteResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.ClienteService;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;
    private final AuditLogService auditLogService;
    private final ModelMapper modelMapper;

    public ClienteServiceImpl(ClienteRepository clienteRepository, UsuarioService usuarioService,
                              AuditLogService auditLogService, ModelMapper modelMapper) {
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
        this.auditLogService = auditLogService;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public ClienteResponseDTO registrar(ClienteRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Cliente cliente = new Cliente();
        cliente.setOwner(usuario);
        applyRequest(cliente, request);
        Cliente saved = clienteRepository.save(cliente);
        auditLogService.registrar(usuario, "CREATE_CLIENT", "Cliente", saved.getClientId(), saved.getFullNameOrCompany());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarPorUsuarioAutenticado() {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return clienteRepository.findByOwnerUserId(usuario.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return toResponse(getCliente(id, usuario.getUserId()));
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Cliente cliente = getCliente(id, usuario.getUserId());
        applyRequest(cliente, request);
        Cliente saved = clienteRepository.saveAndFlush(cliente);
        auditLogService.registrar(usuario, "UPDATE_CLIENT", "Cliente", saved.getClientId(), saved.getFullNameOrCompany());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Cliente cliente = getCliente(id, usuario.getUserId());
        clienteRepository.delete(cliente);
        auditLogService.registrar(usuario, "DELETE_CLIENT", "Cliente", id, cliente.getFullNameOrCompany());
    }

    private Cliente getCliente(Long id, Long userId) {
        return clienteRepository.findByClientIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private void applyRequest(Cliente cliente, ClienteRequestDTO request) {
        modelMapper.map(request, cliente);
    }

    private ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getClientId(),
                cliente.getClientType(),
                cliente.getFullNameOrCompany(),
                cliente.getDocumentNumber(),
                cliente.getEmail(),
                cliente.getPhone(),
                cliente.getAddress(),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }
}
