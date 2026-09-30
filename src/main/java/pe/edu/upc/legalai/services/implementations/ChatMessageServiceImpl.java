package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.chatmessage.ChatMessageRequestDTO;
import pe.edu.upc.legalai.dtos.chatmessage.ChatMessageResponseDTO;
import pe.edu.upc.legalai.entities.ChatMessage;
import pe.edu.upc.legalai.entities.ChatSession;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.ChatMessageRepository;
import pe.edu.upc.legalai.repositories.ChatSessionRepository;
import pe.edu.upc.legalai.services.interfaces.ChatMessageService;

import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

    public ChatMessageServiceImpl(ChatMessageRepository chatMessageRepository,
                                  ChatSessionRepository chatSessionRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    @Override
    @Transactional
    public ChatMessageResponseDTO create(ChatMessageRequestDTO request) {
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión de chat no encontrada con id: " + request.getSessionId()));
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setSenderType(request.getSenderType());
        message.setContent(request.getContent());
        return toResponse(chatMessageRepository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> findAll() {
        return chatMessageRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessageResponseDTO findById(Long id) {
        return toResponse(getMessage(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> findBySessionId(Long sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Sesión de chat no encontrada con id: " + sessionId);
        }
        return chatMessageRepository.findBySessionSessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ChatMessage message = getMessage(id);
        chatMessageRepository.delete(message);
    }

    private ChatMessage getMessage(Long id) {
        return chatMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado con id: " + id));
    }

    private ChatMessageResponseDTO toResponse(ChatMessage message) {
        return new ChatMessageResponseDTO(
                message.getMessageId(),
                message.getSession().getSessionId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
