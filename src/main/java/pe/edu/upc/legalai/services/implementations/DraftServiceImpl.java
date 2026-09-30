package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.draft.DraftRequestDTO;
import pe.edu.upc.legalai.dtos.draft.DraftResponseDTO;
import pe.edu.upc.legalai.entities.CaseFile;
import pe.edu.upc.legalai.entities.Draft;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.CaseFileRepository;
import pe.edu.upc.legalai.repositories.DraftRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.DraftService;

import java.util.List;

@Service
public class DraftServiceImpl implements DraftService {

    private final DraftRepository draftRepository;
    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;

    public DraftServiceImpl(DraftRepository draftRepository, CaseFileRepository caseFileRepository,
                            UserRepository userRepository) {
        this.draftRepository = draftRepository;
        this.caseFileRepository = caseFileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DraftResponseDTO create(DraftRequestDTO request) {
        Draft draft = new Draft();
        draft.setCaseFile(getCaseFile(request.getCaseId()));
        draft.setCreatedBy(getUser(request.getCreatedBy()));
        applyRequest(draft, request);
        return toResponse(draftRepository.save(draft));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftResponseDTO> findAll() {
        return draftRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DraftResponseDTO findById(Long id) {
        return toResponse(getDraft(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftResponseDTO> findByCaseId(Long caseId) {
        if (!caseFileRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Expediente no encontrado con id: " + caseId);
        }
        return draftRepository.findByCaseFileCaseId(caseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DraftResponseDTO update(Long id, DraftRequestDTO request) {
        Draft draft = getDraft(id);
        applyRequest(draft, request);
        return toResponse(draftRepository.save(draft));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Draft draft = getDraft(id);
        draftRepository.delete(draft);
    }

    private void applyRequest(Draft draft, DraftRequestDTO request) {
        draft.setTitle(request.getTitle());
        draft.setPrompt(request.getPrompt());
        draft.setContent(request.getContent());
        if (request.getStatus() != null) {
            draft.setStatus(request.getStatus());
        }
    }

    private Draft getDraft(Long id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrador no encontrado con id: " + id));
    }

    private CaseFile getCaseFile(Long id) {
        return caseFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con id: " + id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private DraftResponseDTO toResponse(Draft draft) {
        return new DraftResponseDTO(
                draft.getDraftId(),
                draft.getCaseFile().getCaseId(),
                draft.getCreatedBy().getUserId(),
                draft.getTitle(),
                draft.getPrompt(),
                draft.getContent(),
                draft.getStatus(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
