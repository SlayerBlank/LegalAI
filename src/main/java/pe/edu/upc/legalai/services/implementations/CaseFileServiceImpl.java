package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.casefile.CaseFileRequestDTO;
import pe.edu.upc.legalai.dtos.casefile.CaseFileResponseDTO;
import pe.edu.upc.legalai.entities.CaseFile;
import pe.edu.upc.legalai.entities.CaseFileStatus;
import pe.edu.upc.legalai.entities.Client;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.CaseFileRepository;
import pe.edu.upc.legalai.repositories.ClientRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.CaseFileService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CaseFileServiceImpl implements CaseFileService {

    private final CaseFileRepository caseFileRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public CaseFileServiceImpl(CaseFileRepository caseFileRepository, ClientRepository clientRepository,
                               UserRepository userRepository) {
        this.caseFileRepository = caseFileRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CaseFileResponseDTO create(CaseFileRequestDTO request) {
        CaseFile caseFile = new CaseFile();
        caseFile.setClient(getClient(request.getClientId()));
        caseFile.setOwnerUser(getUser(request.getOwnerUserId()));
        applyRequest(caseFile, request);
        caseFile.setOpenedAt(LocalDateTime.now());
        return toResponse(caseFileRepository.save(caseFile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseFileResponseDTO> findAll() {
        return caseFileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CaseFileResponseDTO findById(Long id) {
        return toResponse(getCaseFile(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseFileResponseDTO> findByClientId(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + clientId);
        }
        return caseFileRepository.findByClientClientId(clientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseFileResponseDTO> findByOwnerUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }
        return caseFileRepository.findByOwnerUserUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CaseFileResponseDTO update(Long id, CaseFileRequestDTO request) {
        CaseFile caseFile = getCaseFile(id);
        caseFile.setClient(getClient(request.getClientId()));
        caseFile.setOwnerUser(getUser(request.getOwnerUserId()));
        applyRequest(caseFile, request);
        if (caseFile.getStatus() == CaseFileStatus.CLOSED && caseFile.getClosedAt() == null) {
            caseFile.setClosedAt(LocalDateTime.now());
        }
        return toResponse(caseFileRepository.save(caseFile));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CaseFile caseFile = getCaseFile(id);
        caseFileRepository.delete(caseFile);
    }

    private void applyRequest(CaseFile caseFile, CaseFileRequestDTO request) {
        caseFile.setTitle(request.getTitle());
        caseFile.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            caseFile.setStatus(request.getStatus());
        }
    }

    private CaseFile getCaseFile(Long id) {
        return caseFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con id: " + id));
    }

    private Client getClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private CaseFileResponseDTO toResponse(CaseFile caseFile) {
        return new CaseFileResponseDTO(
                caseFile.getCaseId(),
                caseFile.getClient().getClientId(),
                caseFile.getOwnerUser().getUserId(),
                caseFile.getTitle(),
                caseFile.getDescription(),
                caseFile.getStatus(),
                caseFile.getOpenedAt(),
                caseFile.getClosedAt(),
                caseFile.getCreatedAt(),
                caseFile.getUpdatedAt()
        );
    }
}
