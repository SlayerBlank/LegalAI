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
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.servicesinterfaces.*;

@Service
public class DocumentoPreparationServiceImpl implements DocumentoPreparationService {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentoPreparationServiceImpl.class);
    private final IDocumentoService documents;
    private final DocumentoProcessingService processing;
    private final DocumentChunkService chunking;
    private final SemanticSearchService semantic;
    private final IDocumentChunkRepository chunks;
    private final IDocumentoRepository documentRows;
    private final ChunkEmbeddingRepository vectors;
    private final IUsuarioService users;
    private final AuditLogService audit;
    private final TransactionTemplate auditTransaction;
    private final TransactionTemplate readTransaction;

    public DocumentoPreparationServiceImpl(IDocumentoService documents, DocumentoProcessingService processing,
            DocumentChunkService chunking, SemanticSearchService semantic, IDocumentChunkRepository chunks,
            ChunkEmbeddingRepository vectors, IUsuarioService users, AuditLogService audit,
            PlatformTransactionManager manager, IDocumentoRepository documentRows) {
        this.documents = documents;
        this.processing = processing;
        this.chunking = chunking;
        this.semantic = semantic;
        this.chunks = chunks;
        this.documentRows = documentRows;
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
            if (DocumentoProcessingService.procesamientoEnCurso(document.getProcessingStatus(),
                    document.getUpdatedAt())) {
                throw new DuplicateResourceException("El documento ya se esta procesando; intente nuevamente en unos minutos");
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
            // HU-029: si fallan los fragmentos o los embeddings, el documento queda en ERROR.
            // Los fallos de extraccion ya los marca DocumentoProcessingServiceImpl.
            if ("CHUNKS".equals(failedStage) || "EMBEDDINGS".equals(failedStage)) {
                marcarError(documentId, failure);
            }
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

    private void marcarError(Long documentId, RuntimeException failure) {
        try {
            auditTransaction.executeWithoutResult(tx -> documentRows.findById(documentId).ifPresent(row -> {
                row.setProcessingStatus(EstadoProcesamiento.ERROR);
                documentRows.saveAndFlush(row);
            }));
        } catch (RuntimeException markFailure) {
            failure.addSuppressed(markFailure);
            LOG.error("No se pudo marcar ERROR en la preparacion documentId={}", documentId);
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
        EstadoProcesamiento status = document.getProcessingStatus();
        if (status == EstadoProcesamiento.PROCESSING) {
            stage = "PROCESSING";
        } else if (status == EstadoProcesamiento.ERROR && !document.getHasExtractedText()) {
            stage = "EXTRACTION_ERROR";
        } else if (status == EstadoProcesamiento.UPLOADED || !document.getHasExtractedText()) {
            stage = "EXTRACTION_REQUIRED";
        } else if (count == 0) {
            // PROCESSED, o ERROR con texto: fallaron los fragmentos (HU-029).
            stage = "CHUNKS_REQUIRED";
        } else {
            try {
                vectors.validateSchema();
                embedded = vectors.countValidChunks(documentId);
                // Con ERROR nunca se considera listo: hay que volver a preparar el documento.
                stage = embedded == count && status == EstadoProcesamiento.PROCESSED
                        ? "READY" : "EMBEDDINGS_REQUIRED";
            } catch (EmbeddingException schemaFailure) {
                stage = "EMBEDDINGS_UNAVAILABLE";
            }
        }
        return new DocumentoPreparationResponseDTO(documentId, document.getProcessingStatus(),
                stage, "READY".equals(stage), count, embedded);
    }
}
