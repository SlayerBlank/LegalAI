package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.summary.SummaryRequestDTO;
import pe.edu.upc.legalai.dtos.summary.SummaryResponseDTO;
import pe.edu.upc.legalai.entities.CaseFile;
import pe.edu.upc.legalai.entities.Document;
import pe.edu.upc.legalai.entities.Summary;
import pe.edu.upc.legalai.entities.SummaryType;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.CaseFileRepository;
import pe.edu.upc.legalai.repositories.DocumentRepository;
import pe.edu.upc.legalai.repositories.SummaryRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.SummaryService;

import java.util.List;

@Service
public class SummaryServiceImpl implements SummaryService {

    private final SummaryRepository summaryRepository;
    private final DocumentRepository documentRepository;
    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;

    public SummaryServiceImpl(SummaryRepository summaryRepository, DocumentRepository documentRepository,
                              CaseFileRepository caseFileRepository, UserRepository userRepository) {
        this.summaryRepository = summaryRepository;
        this.documentRepository = documentRepository;
        this.caseFileRepository = caseFileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SummaryResponseDTO create(SummaryRequestDTO request) {
        Document document = null;
        CaseFile caseFile = null;

        if (request.getSummaryType() == SummaryType.DOCUMENT) {
            if (request.getDocumentId() == null) {
                throw new BadRequestException("El documento es obligatorio para un resumen de tipo DOCUMENT");
            }
            document = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Documento no encontrado con id: " + request.getDocumentId()));
        } else if (request.getSummaryType() == SummaryType.CASE) {
            if (request.getCaseId() == null) {
                throw new BadRequestException("El expediente es obligatorio para un resumen de tipo CASE");
            }
            caseFile = caseFileRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Expediente no encontrado con id: " + request.getCaseId()));
        }

        User generatedBy = userRepository.findById(request.getGeneratedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + request.getGeneratedBy()));

        Summary summary = new Summary();
        summary.setDocument(document);
        summary.setCaseFile(caseFile);
        summary.setGeneratedBy(generatedBy);
        summary.setSummaryType(request.getSummaryType());
        summary.setContent(request.getContent());
        return toResponse(summaryRepository.save(summary));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummaryResponseDTO> findAll() {
        return summaryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryResponseDTO findById(Long id) {
        return toResponse(getSummary(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummaryResponseDTO> findByDocumentId(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Documento no encontrado con id: " + documentId);
        }
        return summaryRepository.findByDocumentDocumentId(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummaryResponseDTO> findByCaseId(Long caseId) {
        if (!caseFileRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Expediente no encontrado con id: " + caseId);
        }
        return summaryRepository.findByCaseFileCaseId(caseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Summary summary = getSummary(id);
        summaryRepository.delete(summary);
    }

    private Summary getSummary(Long id) {
        return summaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resumen no encontrado con id: " + id));
    }

    private SummaryResponseDTO toResponse(Summary summary) {
        return new SummaryResponseDTO(
                summary.getSummaryId(),
                summary.getDocument() != null ? summary.getDocument().getDocumentId() : null,
                summary.getCaseFile() != null ? summary.getCaseFile().getCaseId() : null,
                summary.getGeneratedBy().getUserId(),
                summary.getSummaryType(),
                summary.getContent(),
                summary.getCreatedAt()
        );
    }
}
