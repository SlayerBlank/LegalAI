package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.document.DocumentRequestDTO;
import pe.edu.upc.legalai.dtos.document.DocumentResponseDTO;
import pe.edu.upc.legalai.entities.Document;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.DocumentRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.DocumentService;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentServiceImpl(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DocumentResponseDTO create(DocumentRequestDTO request) {
        User uploadedBy = getUser(request.getUploadedByUserId());
        Document document = new Document();
        document.setUploadedBy(uploadedBy);
        applyRequest(document, request);
        return toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> findAll() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO findById(Long id) {
        return toResponse(getDocument(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> findByUploadedByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }
        return documentRepository.findByUploadedByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponseDTO update(Long id, DocumentRequestDTO request) {
        Document document = getDocument(id);
        User uploadedBy = getUser(request.getUploadedByUserId());
        document.setUploadedBy(uploadedBy);
        applyRequest(document, request);
        return toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Document document = getDocument(id);
        documentRepository.delete(document);
    }

    private void applyRequest(Document document, DocumentRequestDTO request) {
        document.setFileName(request.getFileName());
        document.setFileType(request.getFileType());
        document.setStorageUrl(request.getStorageUrl());
        document.setCategory(request.getCategory());
        document.setSizeBytes(request.getSizeBytes());
        document.setExtractedText(request.getExtractedText());
        document.setProcessed(request.getProcessed() != null ? request.getProcessed() : false);
    }

    private Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private DocumentResponseDTO toResponse(Document document) {
        return new DocumentResponseDTO(
                document.getDocumentId(),
                document.getUploadedBy().getUserId(),
                document.getFileName(),
                document.getFileType(),
                document.getStorageUrl(),
                document.getCategory(),
                document.getSizeBytes(),
                document.getExtractedText(),
                document.isProcessed(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
