package pe.edu.upc.legalai.servicesimplements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upc.legalai.dtos.response.DocumentoPreparationResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.EmbeddingException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.exceptions.UnauthorizedException;
import pe.edu.upc.legalai.repositories.ChunkEmbeddingRepository;
import pe.edu.upc.legalai.repositories.IDocumentChunkRepository;
import pe.edu.upc.legalai.servicesinterfaces.*;

@Service
public class DocumentoPreparationServiceImpl implements DocumentoPreparationService {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentoPreparationServiceImpl.class);
    private final IDocumentoService documents;
    private final DocumentoProcessingService processing;
    private final DocumentChunkService chunking;
    private final SemanticSearchService semantic;
    private final IDocumentChunkRepository chunks;
    private final ChunkEmbeddingRepository vectors;
    private final IUsuarioService users;
    private final AuditLogService audit;
    private final TransactionTemplate auditTransaction;
    private final TransactionTemplate readTransaction;

    public DocumentoPreparationServiceImpl(IDocumentoService documents, DocumentoProcessingService processing,
            DocumentChunkService chunking, SemanticSearchService semantic, IDocumentChunkRepository chunks,
            ChunkEmbeddingRepository vectors, IUsuarioService users, AuditLogService audit,
            PlatformTransactionManager manager) {
        this.documents = documents;
        this.processing = processing;
        this.chunking = chunking;
        this.semantic = semantic;
        this.chunks = chunks;
        this.vectors = vectors;
        this.users = users;
        this.audit = audit;
        this.auditTransaction = new TransactionTemplate(manager);
        this.auditTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readTransaction = new TransactionTemplate(manager);
        this.readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readTransaction.setReadOnly(true);
        this.readTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DocumentoResponseDTO subirYPreparar(Long caseId, MultipartFile file, String category) {
        // The upload transaction commits before any parsing or provider call can fail.
        var saved = documents.subirArchivo(caseId, file, category);
        try {
            prepareStages(saved.getDocumentId());
        } catch (ResourceNotFoundException | UnauthorizedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOG.warn("Documento conservado; preparacion pendiente documentId={} type={}",
                    saved.getDocumentId(), ex.getClass().getSimpleName());
        }
        // Keep the existing upload JSON contract. Read readiness through /preparation.
        return documents.buscarPorId(saved.getDocumentId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DocumentoPreparationResponseDTO preparar(Long documentId) {
        prepareStages(documentId);
        return estado(documentId);
    }

    private void prepareStages(Long documentId) {
        var document = documents.buscarPorId(documentId); // Ownership before all work and audit.
        var user = users.obtenerUsuarioAutenticado();
        String stage = "EXTRACTION";
        try {
            if (document.getProcessingStatus() == EstadoProcesamiento.PROCESSING) {
                throw new DuplicateResourceException("El documento ya se esta procesando");
            }
            if (document.getProcessingStatus() != EstadoProcesamiento.PROCESSED || !document.getHasExtractedText()) {
                processing.procesar(documentId);
            }
            stage = "CHUNKS";
            if (chunks.countByDocumentoDocumentId(documentId) == 0) {
                chunking.generar(documentId);
            }
            stage = "EMBEDDINGS";
            // Existing implementation generates only missing/invalid vectors, atomically.
            semantic.generar(documentId, false);
            stage = "AUDIT";
            auditTransaction.executeWithoutResult(tx -> audit.registrar(user, "PREPARE_DOCUMENT", "Documento",
                    documentId, "stage=EMBEDDINGS; result=SUCCESS"));
        } catch (RuntimeException failure) {
            String failedStage = stage;
            try {
                auditTransaction.executeWithoutResult(tx -> audit.registrar(user, "PREPARE_DOCUMENT", "Documento",
                        documentId, "stage=" + failedStage + "; result=FAILED; type=" + failure.getClass().getSimpleName()));
            } catch (RuntimeException auditFailure) {
                failure.addSuppressed(auditFailure);
                LOG.error("No se pudo auditar preparacion documentId={} stage={}", documentId, failedStage);
            }
            throw failure;
        }
    }

    @Override
    public DocumentoPreparationResponseDTO estado(Long documentId) {
        // A single snapshot also applies when called after preparar(), without relying
        // on self-invocation through a Spring transaction proxy.
        return java.util.Objects.requireNonNull(readTransaction.execute(tx -> readState(documentId)));
    }

    private DocumentoPreparationResponseDTO readState(Long documentId) {
        var document = documents.buscarPorId(documentId);
        long count = chunks.countByDocumentoDocumentId(documentId);
        long embedded = 0;
        String stage;
        if (document.getProcessingStatus() == EstadoProcesamiento.PROCESSING) {
            stage = "PROCESSING";
        } else if (document.getProcessingStatus() == EstadoProcesamiento.ERROR) {
            stage = "EXTRACTION_ERROR";
        } else if (document.getProcessingStatus() != EstadoProcesamiento.PROCESSED || !document.getHasExtractedText()) {
            stage = "EXTRACTION_REQUIRED";
        } else if (count == 0) {
            stage = "CHUNKS_REQUIRED";
        } else {
            try {
                vectors.validateSchema();
                embedded = vectors.countValidChunks(documentId);
                stage = embedded == count ? "READY" : "EMBEDDINGS_REQUIRED";
            } catch (EmbeddingException schemaFailure) {
                stage = "EMBEDDINGS_UNAVAILABLE";
            }
        }
        return new DocumentoPreparationResponseDTO(documentId, document.getProcessingStatus(),
                stage, "READY".equals(stage), count, embedded);
    }
}
