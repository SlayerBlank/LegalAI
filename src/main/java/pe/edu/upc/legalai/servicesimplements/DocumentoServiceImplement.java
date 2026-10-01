package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.dtos.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.DocumentUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentoServiceImplement implements IDocumentoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentoServiceImplement.class);
    private final Path storageRoot;
    private final long maxFileSize;

    private final IDocumentoRepository documentoRepository;
    private final IExpedienteRepository expedienteRepository;
    private final IUsuarioService usuarioService;
    private final AuditLogService auditLogService;

    public DocumentoServiceImplement(IDocumentoRepository documentoRepository, IExpedienteRepository expedienteRepository,
                                IUsuarioService usuarioService, AuditLogService auditLogService,
                                @Value("${legalai.storage.path}") String storagePath,
                                @Value("${legalai.documents.max-file-size}") DataSize maxFileSize) {
        this.documentoRepository = documentoRepository;
        this.expedienteRepository = expedienteRepository;
        this.usuarioService = usuarioService;
        this.auditLogService = auditLogService;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize.toBytes();
        if (this.maxFileSize <= 0) {
            throw new IllegalArgumentException("El limite de archivos debe ser positivo");
        }
    }

    @Override
    @Transactional
    public DocumentoResponseDTO subirArchivo(Long caseId, MultipartFile file, String category) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Expediente expediente = getExpediente(caseId, usuario.getUserId());
        validateUpload(file, category);
        Path target = null;
        boolean created = false;
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            // Signature only: no PDF parsing or text extraction in this phase.
            if (header.length != 8 || header[0] != '%' || header[1] != 'P' || header[2] != 'D'
                    || header[3] != 'F' || header[4] != '-'
                    || (header[5] != '1' && header[5] != '2') || header[6] != '.'
                    || header[7] < '0' || header[7] > '9') {
                throw new BadRequestException("El contenido del archivo no tiene una cabecera PDF valida");
            }
            Files.createDirectories(storageRoot);
            Path realRoot = storageRoot.toRealPath();
            String storageId = UUID.randomUUID() + ".pdf";
            target = realRoot.resolve(storageId).normalize();
            if (!target.startsWith(realRoot)) {
                throw new BadRequestException("Nombre de archivo invalido");
            }
            long written = header.length;
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                Path createdFile = target;
                // Also handles failures occurring at commit, after this method returns.
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                cleanup(createdFile);
                            } else if (status == STATUS_UNKNOWN) {
                                LOGGER.error("Resultado de transaccion desconocido para archivo {}; requiere conciliacion", storageId);
                            }
                        }
                    });
                }
                output.write(header);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    written += count;
                    if (written > maxFileSize) {
                        throw new BadRequestException("El archivo supera el tamano maximo permitido");
                    }
                    output.write(buffer, 0, count);
                }
            }
            if (written != file.getSize()) {
                throw new BadRequestException("El tamano del archivo no coincide con su contenido");
            }
            Documento documento = new Documento();
            documento.setExpediente(expediente);
            documento.setUploadedBy(usuario);
            documento.setFileName(file.getOriginalFilename());
            documento.setFileType("application/pdf");
            documento.setStorageUrl(storageId);
            documento.setSizeBytes(written);
            documento.setCategory(category);
            documento.setProcessingStatus(EstadoProcesamiento.UPLOADED);
            Documento saved = documentoRepository.saveAndFlush(documento);
            auditLogService.registrar(usuario, "UPLOAD_DOCUMENT", "Documento", saved.getDocumentId(),
                    "caseId=" + caseId + "; fileName=" + saved.getFileName());
            return toResponse(saved);
        } catch (BadRequestException ex) {
            if (created) cleanup(target);
            throw ex;
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("Error al guardar documento. caseId={}, fileName={}", caseId, file.getOriginalFilename(), ex);
            if (created) cleanup(target);
            throw new DocumentUploadException(ex);
        }
    }

    private void validateUpload(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo PDF es obligatorio y no puede estar vacio");
        }
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("El archivo supera el tamano maximo permitido");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 255
                || name.chars().anyMatch(c -> Character.isISOControl(c) || "<>:\"/\\|?*".indexOf(c) >= 0)) {
            throw new BadRequestException("Nombre de archivo invalido");
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Solo se permiten archivos PDF con Content-Type application/pdf");
        }
        if (category != null && category.length() > 80) {
            throw new BadRequestException("La categoria no debe superar los 80 caracteres");
        }
    }

    private void cleanup(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("No se pudo eliminar archivo de carga fallida {}; requiere conciliacion", file.getFileName());
        }
    }

    @Override
    @Transactional
    public DocumentoResponseDTO registrar(Long caseId, DocumentoRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Expediente expediente = getExpediente(caseId, usuario.getUserId());
        Documento documento = new Documento();
        documento.setExpediente(expediente);
        documento.setUploadedBy(usuario);
        documento.setProcessingStatus(EstadoProcesamiento.UPLOADED);
        applyRequest(documento, request);
        Documento saved = documentoRepository.save(documento);
        auditLogService.registrar(usuario, "UPLOAD_DOCUMENT", "Documento", saved.getDocumentId(), saved.getFileName());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listarPorExpediente(Long caseId) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        getExpediente(caseId, usuario.getUserId());
        return documentoRepository.findByExpedienteCaseIdAndExpedienteOwnerUserId(caseId, usuario.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return toResponse(getDocumento(id, usuario.getUserId()));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Documento documento = getDocumento(id, usuario.getUserId());
        documentoRepository.delete(documento);
        auditLogService.registrar(usuario, "DELETE_DOCUMENT", "Documento", id, documento.getFileName());
    }

    private Expediente getExpediente(Long caseId, Long userId) {
        return expedienteRepository.findByCaseIdAndOwnerUserId(caseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado"));
    }

    private Documento getDocumento(Long documentId, Long userId) {
        return documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
    }

    private void applyRequest(Documento documento, DocumentoRequestDTO request) {
        documento.setFileName(request.getFileName());
        documento.setFileType(request.getFileType());
        documento.setStorageUrl(request.getStorageUrl());
        documento.setCategory(request.getCategory());
        documento.setSizeBytes(request.getSizeBytes());
    }

    private DocumentoResponseDTO toResponse(Documento documento) {
        DocumentoResponseDTO response = new DocumentoResponseDTO(
                documento.getDocumentId(),
                documento.getExpediente().getCaseId(),
                documento.getFileName(),
                documento.getFileType(),
                documento.getStorageUrl(),
                documento.getCategory(),
                documento.getSizeBytes(),
                documento.getProcessingStatus(),
                documento.getCreatedAt(),
                documento.getUpdatedAt()
        );
        response.setExtractedTextLength(documento.getExtractedText() == null ? 0 : documento.getExtractedText().length());
        return response;
    }
}
