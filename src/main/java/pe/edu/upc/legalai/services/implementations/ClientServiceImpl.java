package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.client.ClientRequestDTO;
import pe.edu.upc.legalai.dtos.client.ClientResponseDTO;
import pe.edu.upc.legalai.entities.Client;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.ClientRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.ClientService;

import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public ClientServiceImpl(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ClientResponseDTO create(ClientRequestDTO request) {
        User owner = getUser(request.getOwnerUserId());
        Client client = new Client();
        client.setOwnerUser(owner);
        applyRequest(client, request);
        return toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> findAll() {
        return clientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDTO findById(Long id) {
        return toResponse(getClient(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> findByOwnerUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }
        return clientRepository.findByOwnerUserUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClientResponseDTO update(Long id, ClientRequestDTO request) {
        Client client = getClient(id);
        User owner = getUser(request.getOwnerUserId());
        client.setOwnerUser(owner);
        applyRequest(client, request);
        return toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Client client = getClient(id);
        clientRepository.delete(client);
    }

    private void applyRequest(Client client, ClientRequestDTO request) {
        client.setClientType(request.getClientType());
        client.setFullNameOrCompany(request.getFullNameOrCompany());
        client.setDocumentNumber(request.getDocumentNumber());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
    }

    private Client getClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
                client.getClientId(),
                client.getOwnerUser().getUserId(),
                client.getClientType(),
                client.getFullNameOrCompany(),
                client.getDocumentNumber(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
