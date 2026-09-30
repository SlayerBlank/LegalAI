package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.documentchunk.DocumentChunkRequestDTO;
import pe.edu.upc.legalai.dtos.documentchunk.DocumentChunkResponseDTO;
import pe.edu.upc.legalai.entities.Document;
import pe.edu.upc.legalai.entities.DocumentChunk;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.DocumentChunkRepository;
import pe.edu.upc.legalai.repositories.DocumentRepository;
import pe.edu.upc.legalai.services.interfaces.DocumentChunkService;

import java.util.List;

@Service
public class DocumentChunkServiceImpl implements DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public DocumentChunkServiceImpl(DocumentChunkRepository documentChunkRepository,
                                    DocumentRepository documentRepository) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional
    public DocumentChunkResponseDTO create(DocumentChunkRequestDTO request) {
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado con id: " + request.getDocumentId()));
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(document);
        chunk.setChunkIndex(request.getChunkIndex());
        chunk.setPageNumber(request.getPageNumber());
        chunk.setContent(request.getContent());
        chunk.setEmbeddingRef(request.getEmbeddingRef());
        return toResponse(documentChunkRepository.save(chunk));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponseDTO> findAll() {
        return documentChunkRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentChunkResponseDTO findById(Long id) {
        return toResponse(getChunk(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponseDTO> findByDocumentId(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Documento no encontrado con id: " + documentId);
        }
        return documentChunkRepository.findByDocumentDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DocumentChunk chunk = getChunk(id);
        documentChunkRepository.delete(chunk);
    }

    private DocumentChunk getChunk(Long id) {
        return documentChunkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fragmento no encontrado con id: " + id));
    }

    private DocumentChunkResponseDTO toResponse(DocumentChunk chunk) {
        return new DocumentChunkResponseDTO(
                chunk.getChunkId(),
                chunk.getDocument().getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getPageNumber(),
                chunk.getContent(),
                chunk.getEmbeddingRef(),
                chunk.getCreatedAt()
        );
    }
}
