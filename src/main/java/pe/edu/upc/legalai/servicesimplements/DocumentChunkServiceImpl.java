package pe.edu.upc.legalai.servicesimplements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.DTOs.response.*;
import pe.edu.upc.legalai.entities.*;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.repositories.*;
import pe.edu.upc.legalai.servicesinterfaces.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkServiceImpl implements DocumentChunkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentChunkServiceImpl.class);
    private final DocumentoRepository documents;
    private final DocumentChunkRepository chunks;
    private final UsuarioService users;
    private final AuditLogService audit;
    private final CharacterChunker chunker;

    public DocumentChunkServiceImpl(DocumentoRepository documents, DocumentChunkRepository chunks,
            UsuarioService users, AuditLogService audit, CharacterChunker chunker) {
        this.documents = documents;
        this.chunks = chunks;
        this.users = users;
        this.audit = audit;
        this.chunker = chunker;
    }

    @Override
    @Transactional
    public ChunkGenerationResponseDTO generar(Long documentId) {
        Usuario user = users.obtenerUsuarioAutenticado();
        try {
            // Same lock as extraction: serialize regeneration and processing for this document.
            Documento document = documents.findOwnedForProcessing(documentId, user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
            if (document.getProcessingStatus() != EstadoProcesamiento.PROCESSED) {
                throw new BadRequestException("El documento debe estar PROCESSED antes de generar chunks");
            }
            if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
                throw new BadRequestException("El documento no tiene texto extraido para generar chunks");
            }
            var fragments = chunker.split(document.getExtractedText());
            if (fragments.isEmpty()) throw new BadRequestException("El documento no tiene texto util para generar chunks");
            List<DocumentChunk> generated = new ArrayList<>(fragments.size());
            for (var fragment : fragments) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumento(document);
                chunk.setChunkIndex(generated.size());
                chunk.setContent(fragment.content());
                chunk.setCharStart(fragment.charStart());
                chunk.setCharEnd(fragment.charEnd());
                generated.add(chunk);
            }
            // Bulk DELETE is executed before INSERTs to respect the unique constraint.
            chunks.deleteByDocumentoDocumentId(documentId);
            chunks.saveAll(generated);
            chunks.flush();
            audit.registrar(user, "GENERATE_DOCUMENT_CHUNKS", "Documento", documentId,
                    "chunksCreated=" + generated.size());
            chunks.flush();
            return new ChunkGenerationResponseDTO(documentId, generated.size(), chunker.getChunkSize(), chunker.getOverlap());
        } catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.error("Error al generar chunks. documentId={}", documentId, ex);
            throw new ChunkGenerationException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponseDTO> listar(Long documentId) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        documents.findByDocumentIdAndExpedienteOwnerUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        return chunks.findByDocumentoDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(chunk -> new DocumentChunkResponseDTO(chunk.getChunkId(), documentId, chunk.getChunkIndex(),
                        chunk.getContent(), chunk.getCharStart(), chunk.getCharEnd())).toList();
    }
}
