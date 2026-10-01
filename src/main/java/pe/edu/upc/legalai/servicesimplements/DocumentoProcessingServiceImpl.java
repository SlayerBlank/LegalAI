package pe.edu.upc.legalai.servicesimplements;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoTextResponseDTO;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IDocumentChunkRepository;
import pe.edu.upc.legalai.servicesinterfaces.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentoProcessingServiceImpl implements DocumentoProcessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentoProcessingServiceImpl.class);
    private final IDocumentoRepository documents;
    private final IDocumentChunkRepository chunks;
    private final IUsuarioService users;
    private final AuditLogService audit;
    private final IDocumentoService metadata;
    private final TransactionTemplate transaction;
    private final Path storageRoot;

    public DocumentoProcessingServiceImpl(IDocumentoRepository documents, IUsuarioService users,
            AuditLogService audit, IDocumentoService metadata, PlatformTransactionManager manager,
            @Value("${legalai.storage.path}") String storagePath, IDocumentChunkRepository chunks) {
        this.documents = documents;
        this.chunks = chunks;
        this.users = users;
        this.audit = audit;
        this.metadata = metadata;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DocumentoResponseDTO procesar(Long documentId) {
        var user = users.obtenerUsuarioAutenticado();
        boolean started = false;
        try {
            String reference = transaction.execute(status -> {
                Documento document = owned(documentId, user.getUserId());
                if (document.getProcessingStatus() == EstadoProcesamiento.PROCESSING) {
                    throw new DuplicateResourceException("El documento ya se esta procesando");
                }
                document.setProcessingStatus(EstadoProcesamiento.PROCESSING);
                document.setExtractedText(null);
                // Chunks derived from the old text must not survive re-extraction.
                chunks.deleteByDocumentoDocumentId(documentId);
                documents.saveAndFlush(document);
                return document.getStorageUrl();
            });
            started = true;
            // No SQL transaction is held while reading/parsing the PDF.
            String text;
            try (var pdf = Loader.loadPDF(resolve(reference).toFile())) {
                if (!pdf.getCurrentAccessPermission().canExtractContent()) {
                    throw new BadRequestException("El PDF no permite extraer su contenido");
                }
                text = new PDFTextStripper().getText(pdf).trim();
            }
            if (text.isBlank()) {
                throw new BadRequestException("No se pudo extraer texto del PDF. El documento puede ser escaneado.");
            }
            transaction.executeWithoutResult(status -> {
                Documento document = owned(documentId, user.getUserId());
                document.setExtractedText(text);
                document.setProcessingStatus(EstadoProcesamiento.PROCESSED);
                documents.saveAndFlush(document);
                audit.registrar(user, "PROCESS_DOCUMENT", "Documento", documentId,
                        "extractedTextLength=" + text.length());
                // Flush audit too: any failure rolls back text and PROCESSED together.
                documents.flush();
            });
        } catch (ResourceNotFoundException | DuplicateResourceException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("Error al procesar documento. documentId={}", documentId, ex);
            if (started) markError(documentId, user.getUserId(), ex);
            if (ex instanceof BadRequestException badRequest) throw badRequest;
            throw new DocumentProcessingException("No se pudo procesar el PDF. Verifique el archivo o contacte al administrador.", ex);
        }
        return metadata.buscarPorId(documentId);
    }

    private void markError(Long id, Long userId, Exception original) {
        try {
            transaction.executeWithoutResult(status -> {
                Documento document = owned(id, userId);
                document.setExtractedText(null);
                document.setProcessingStatus(EstadoProcesamiento.ERROR);
                documents.saveAndFlush(document);
            });
        } catch (RuntimeException failure) {
            original.addSuppressed(failure);
            LOGGER.error("No se pudo persistir ERROR. documentId={}; requiere revision", id, failure);
        }
    }

    private Documento owned(Long id, Long userId) {
        return documents.findOwnedForProcessing(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
    }

    private Path resolve(String reference) throws IOException {
        if (reference == null || reference.isBlank() || reference.contains("\\") || reference.contains(":")) {
            throw new IOException("Referencia de storage invalida");
        }
        Path relative = Path.of(reference);
        if (relative.isAbsolute() || relative.getNameCount() > 2) throw new IOException("Referencia de storage invalida");
        for (Path component : relative) {
            if (component.toString().equals("..") || component.toString().equals(".")) {
                throw new IOException("Referencia de storage invalida");
            }
        }
        if (relative.getNameCount() == 2) {
            if (!relative.getName(0).toString().equals("uploads")
                    && !relative.getName(0).equals(storageRoot.getFileName())) {
                throw new IOException("Prefijo de storage invalido");
            }
            relative = relative.getFileName();
        }
        Path root = storageRoot.toRealPath();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new IOException("Archivo fuera del storage");
        Path real = target.toRealPath();
        if (!real.startsWith(root) || !Files.isRegularFile(real) || !Files.isReadable(real)) {
            throw new IOException("Archivo PDF no disponible en storage");
        }
        return real;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoTextResponseDTO obtenerTexto(Long documentId) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        Documento document = documents.findByDocumentIdAndExpedienteOwnerUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        return new DocumentoTextResponseDTO(documentId, document.getExtractedText());
    }
}
