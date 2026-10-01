package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.request.SemanticSearchRequestDTO;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.configs.EmbeddingSettings;
import pe.edu.upc.legalai.entities.DocumentChunk;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.repositories.*;
import pe.edu.upc.legalai.servicesinterfaces.*;
import java.util.List;

@Service
public class SemanticSearchService {
    private final IDocumentoRepository documents;
    private final IExpedienteRepository cases;
    private final IDocumentChunkRepository chunks;
    private final ChunkEmbeddingRepository vectors;
    private final IUsuarioService users;
    private final EmbeddingService embeddings;
    private final EmbeddingSettings settings;

    public SemanticSearchService(IDocumentoRepository documents, IExpedienteRepository cases,
            IDocumentChunkRepository chunks, ChunkEmbeddingRepository vectors, IUsuarioService users,
            EmbeddingService embeddings, EmbeddingSettings settings) {
        this.documents = documents;
        this.cases = cases;
        this.chunks = chunks;
        this.vectors = vectors;
        this.users = users;
        this.embeddings = embeddings;
        this.settings = settings;
    }

    @Transactional
    public EmbeddingGenerationResponseDTO generar(Long documentId, boolean force) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        documents.findOwnedForProcessing(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        List<DocumentChunk> all = chunks.findByDocumentoDocumentIdOrderByChunkIndexAsc(documentId);
        if (all.isEmpty()) throw new BadRequestException("El documento no tiene chunks; genere los chunks primero");
        vectors.validateSchema();
        var valid = vectors.validChunkIds(documentId);
        var pending = all.stream().filter(c -> force || !valid.contains(c.getChunkId())).toList();
        int generated = 0;
        for (int offset = 0; offset < pending.size(); offset += settings.batchSize()) {
            var batch = pending.subList(offset, Math.min(offset + settings.batchSize(), pending.size()));
            try {
                var result = embeddings.generarEmbeddings(batch.stream().map(DocumentChunk::getContent).toList());
                if (result == null || result.size() != batch.size())
                    throw new EmbeddingException("Lote de embeddings incompleto");
                for (int i = 0; i < batch.size(); i++) {
                    settings.validate(result.get(i));
                    vectors.save(documentId, batch.get(i).getChunkId(), result.get(i));
                    generated++;
                }
            } catch (RuntimeException ex) {
                String reason = ex instanceof EmbeddingException ? ex.getMessage() : "Fallo al persistir embeddings";
                throw new EmbeddingException("Operacion revertida; chunks afectados "
                        + batch.stream().map(DocumentChunk::getChunkId).toList() + ": " + reason);
            }
        }
        return new EmbeddingGenerationResponseDTO(documentId, all.size(), generated);
    }

    @Transactional(readOnly = true)
    public List<SemanticSearchResultDTO> searchDocument(Long id, SemanticSearchRequestDTO request) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        documents.findByDocumentIdAndExpedienteOwnerUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        return vectors.searchDocument(id, userId, query(request), request.getTopK());
    }

    @Transactional(readOnly = true)
    public List<SemanticSearchResultDTO> searchCase(Long id, SemanticSearchRequestDTO request) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        cases.findByCaseIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado"));
        return vectors.searchCase(id, userId, query(request), request.getTopK());
    }

    private float[] query(SemanticSearchRequestDTO request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()
                || request.getTopK() == null || request.getTopK() < 1 || request.getTopK() > 20)
            throw new BadRequestException("query es obligatoria y topK debe estar entre 1 y 20");
        vectors.validateSchema();
        float[] vector = embeddings.generarEmbeddingConsulta(request.getQuery().trim());
        settings.validate(vector);
        return vector;
    }
}
