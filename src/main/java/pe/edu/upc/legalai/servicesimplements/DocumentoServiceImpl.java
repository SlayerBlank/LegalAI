package pe.edu.upc.legalai.servicesimplements;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.DocumentoRepository;
import pe.edu.upc.legalai.repositories.ExpedienteRepository;
import pe.edu.upc.legalai.DTOs.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.DTOs.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

import java.util.List;

@Service
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final UsuarioService usuarioService;
    private final AuditLogService auditLogService;
    private final ModelMapper modelMapper;

    public DocumentoServiceImpl(DocumentoRepository documentoRepository, ExpedienteRepository expedienteRepository,
                                UsuarioService usuarioService, AuditLogService auditLogService, ModelMapper modelMapper) {
        this.documentoRepository = documentoRepository;
        this.expedienteRepository = expedienteRepository;
        this.usuarioService = usuarioService;
        this.auditLogService = auditLogService;
        this.modelMapper = modelMapper;
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
        modelMapper.map(request, documento);
    }

    private DocumentoResponseDTO toResponse(Documento documento) {
        return new DocumentoResponseDTO(
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
    }
}
