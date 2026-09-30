package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.chatsession.ChatSessionRequestDTO;
import pe.edu.upc.legalai.dtos.chatsession.ChatSessionResponseDTO;
import pe.edu.upc.legalai.entities.CaseFile;
import pe.edu.upc.legalai.entities.ChatSession;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.CaseFileRepository;
import pe.edu.upc.legalai.repositories.ChatSessionRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.ChatSessionService;

import java.util.List;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;

    public ChatSessionServiceImpl(ChatSessionRepository chatSessionRepository, CaseFileRepository caseFileRepository,
                                  UserRepository userRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.caseFileRepository = caseFileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO create(ChatSessionRequestDTO request) {
        ChatSession session = new ChatSession();
        session.setCaseFile(getCaseFile(request.getCaseId()));
        session.setUser(getUser(request.getUserId()));
        session.setTitle(request.getTitle());
        return toResponse(chatSessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO> findAll() {
        return chatSessionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionResponseDTO findById(Long id) {
        return toResponse(getSession(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO> findByCaseId(Long caseId) {
        if (!caseFileRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Expediente no encontrado con id: " + caseId);
        }
        return chatSessionRepository.findByCaseFileCaseId(caseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO> findByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }
        return chatSessionRepository.findByUserUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO update(Long id, ChatSessionRequestDTO request) {
        ChatSession session = getSession(id);
        session.setCaseFile(getCaseFile(request.getCaseId()));
        session.setUser(getUser(request.getUserId()));
        session.setTitle(request.getTitle());
        return toResponse(chatSessionRepository.save(session));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ChatSession session = getSession(id);
        chatSessionRepository.delete(session);
    }

    private ChatSession getSession(Long id) {
        return chatSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de chat no encontrada con id: " + id));
    }

    private CaseFile getCaseFile(Long id) {
        return caseFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con id: " + id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private ChatSessionResponseDTO toResponse(ChatSession session) {
        return new ChatSessionResponseDTO(
                session.getSessionId(),
                session.getCaseFile().getCaseId(),
                session.getUser().getUserId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
