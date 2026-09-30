package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.aicitation.AiCitationRequestDTO;
import pe.edu.upc.legalai.dtos.aicitation.AiCitationResponseDTO;
import pe.edu.upc.legalai.entities.AiCitation;
import pe.edu.upc.legalai.entities.ChatMessage;
import pe.edu.upc.legalai.entities.Document;
import pe.edu.upc.legalai.entities.DocumentChunk;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.AiCitationRepository;
import pe.edu.upc.legalai.repositories.ChatMessageRepository;
import pe.edu.upc.legalai.repositories.DocumentChunkRepository;
import pe.edu.upc.legalai.repositories.DocumentRepository;
import pe.edu.upc.legalai.services.interfaces.AiCitationService;

import java.util.List;

@Service
public class AiCitationServiceImpl implements AiCitationService {

    private final AiCitationRepository aiCitationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public AiCitationServiceImpl(AiCitationRepository aiCitationRepository, ChatMessageRepository chatMessageRepository,
                                 DocumentRepository documentRepository, DocumentChunkRepository documentChunkRepository) {
        this.aiCitationRepository = aiCitationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Override
    @Transactional
    public AiCitationResponseDTO create(AiCitationRequestDTO request) {
        ChatMessage message = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mensaje no encontrado con id: " + request.getMessageId()));
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado con id: " + request.getDocumentId()));
        DocumentChunk chunk = documentChunkRepository.findById(request.getChunkId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fragmento no encontrado con id: " + request.getChunkId()));

        AiCitation citation = new AiCitation();
        citation.setMessage(message);
        citation.setDocument(document);
        citation.setChunk(chunk);
        citation.setRelevanceScore(request.getRelevanceScore());
        return toResponse(aiCitationRepository.save(citation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiCitationResponseDTO> findAll() {
        return aiCitationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AiCitationResponseDTO findById(Long id) {
        return toResponse(getCitation(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiCitationResponseDTO> findByMessageId(Long messageId) {
        if (!chatMessageRepository.existsById(messageId)) {
            throw new ResourceNotFoundException("Mensaje no encontrado con id: " + messageId);
        }
        return aiCitationRepository.findByMessageMessageId(messageId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AiCitation citation = getCitation(id);
        aiCitationRepository.delete(citation);
    }

    private AiCitation getCitation(Long id) {
        return aiCitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));
    }

    private AiCitationResponseDTO toResponse(AiCitation citation) {
        return new AiCitationResponseDTO(
                citation.getCitationId(),
                citation.getMessage().getMessageId(),
                citation.getDocument().getDocumentId(),
                citation.getChunk().getChunkId(),
                citation.getRelevanceScore(),
                citation.getCreatedAt()
        );
    }
}
