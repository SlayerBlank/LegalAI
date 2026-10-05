package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.request.SummariesRequestDTO;
import pe.edu.upc.legalai.dtos.response.SummariesResponseDTO;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Summaries;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.repositories.ISummariesRepository;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.ISummariesService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;
import pe.edu.upc.legalai.dtos.response.SummariesConsultaResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class SummariesServiceImplement implements ISummariesService {

    private final ISummariesRepository summariesRepository;
    private final IDocumentoRepository documentoRepository;
    private final IExpedienteRepository expedienteRepository;
    private final IUsuarioService usuarioService;
    private final AuditLogService auditLogService;

    public SummariesServiceImplement(
            ISummariesRepository summariesRepository,
            IDocumentoRepository documentoRepository,
            IExpedienteRepository expedienteRepository,
            IUsuarioService usuarioService,
            AuditLogService auditLogService
    ) {
        this.summariesRepository = summariesRepository;
        this.documentoRepository = documentoRepository;
        this.expedienteRepository = expedienteRepository;
        this.usuarioService = usuarioService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public SummariesResponseDTO registrar(
            SummariesRequestDTO request
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Summaries summary = new Summaries();
        aplicarDatos(summary, request, usuario.getUserId());
        summary.setGeneratedBy(usuario);

        Summaries guardado = summariesRepository.saveAndFlush(summary);

        auditLogService.registrar(
                usuario,
                "CREATE_SUMMARY",
                "Summaries",
                guardado.getSummaryId(),
                "Resumen creado"
        );

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummariesResponseDTO> listarPorUsuarioAutenticado() {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Long userId = usuario.getUserId();

        return summariesRepository
                .findByGeneratedByUserIdOrderBySummaryIdDesc(userId)
                .stream()
                .filter(summary -> tieneRelacionesPropias(summary, userId))
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SummariesResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Summaries summary = obtenerResumenPropio(
                id,
                usuario.getUserId()
        );

        return convertirAResponse(summary);
    }

    @Override
    @Transactional
    public SummariesResponseDTO actualizar(
            Long id,
            SummariesRequestDTO request
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Summaries summary = obtenerResumenPropio(
                id,
                usuario.getUserId()
        );

        aplicarDatos(summary, request, usuario.getUserId());

        Summaries guardado = summariesRepository.saveAndFlush(summary);

        auditLogService.registrar(
                usuario,
                "UPDATE_SUMMARY",
                "Summaries",
                guardado.getSummaryId(),
                "Resumen actualizado"
        );

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Summaries summary = obtenerResumenPropio(
                id,
                usuario.getUserId()
        );

        auditLogService.registrar(
                usuario,
                "DELETE_SUMMARY",
                "Summaries",
                summary.getSummaryId(),
                "Resumen eliminado"
        );

        summariesRepository.delete(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummariesConsultaResponseDTO> consultarPorExpedienteTipoYFecha(
            Long caseId,
            String summaryType,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        if (caseId == null || caseId <= 0) {
            throw new BadRequestException(
                    "El ID del expediente debe ser positivo"
            );
        }

        if (summaryType == null || summaryType.isBlank()) {
            throw new BadRequestException(
                    "El tipo de resumen es obligatorio"
            );
        }

        if (createdFrom == null || createdTo == null) {
            throw new BadRequestException(
                    "Las fechas inicial y final son obligatorias"
            );
        }

        if (createdFrom.isAfter(createdTo)) {
            throw new BadRequestException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        expedienteRepository
                .findByCaseIdAndOwnerUserId(
                        caseId,
                        usuario.getUserId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expediente no encontrado"
                ));

        LocalDateTime fechaInicial = createdFrom.atStartOfDay();
        LocalDateTime fechaFinalExclusiva = createdTo
                .plusDays(1)
                .atStartOfDay();

        return summariesRepository.consultarPorExpedienteTipoYFecha(
                usuario.getUserId(),
                caseId,
                summaryType,
                fechaInicial,
                fechaFinalExclusiva
        );
    }

    private Summaries obtenerResumenPropio(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BadRequestException(
                    "El ID del resumen debe ser positivo"
            );
        }

        Summaries summary = summariesRepository
                .findBySummaryIdAndGeneratedByUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resumen no encontrado"
                ));

        if (!tieneRelacionesPropias(summary, userId)) {
            throw new ResourceNotFoundException(
                    "Resumen no encontrado"
            );
        }

        return summary;
    }

    private void aplicarDatos(
            Summaries summary,
            SummariesRequestDTO request,
            Long userId
    ) {
        if (request.getDocumentId() == null
                && request.getCaseId() == null) {
            throw new BadRequestException(
                    "Debe indicar documentId o caseId"
            );
        }

        Documento documento = null;
        Expediente expediente = null;

        if (request.getDocumentId() != null) {
            documento = documentoRepository
                    .findByDocumentIdAndExpedienteOwnerUserId(
                            request.getDocumentId(),
                            userId
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Documento no encontrado"
                    ));
        }

        if (request.getCaseId() != null) {
            expediente = expedienteRepository
                    .findByCaseIdAndOwnerUserId(
                            request.getCaseId(),
                            userId
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Expediente no encontrado"
                    ));
        }

        if (documento != null && expediente != null) {
            Long caseIdDelDocumento = documento
                    .getExpediente()
                    .getCaseId();

            if (!caseIdDelDocumento.equals(expediente.getCaseId())) {
                throw new BadRequestException(
                        "El documento no pertenece al expediente indicado"
                );
            }
        }

        summary.setDocumento(documento);
        summary.setExpediente(expediente);
        summary.setSummaryType(request.getSummaryType());
        summary.setContent(request.getContent());
    }

    private boolean tieneRelacionesPropias(
            Summaries summary,
            Long userId
    ) {
        Expediente expediente = summary.getExpediente();
        Documento documento = summary.getDocumento();

        if (expediente == null && documento == null) {
            return false;
        }

        if (expediente != null
                && !expediente.getOwner().getUserId().equals(userId)) {
            return false;
        }

        if (documento != null) {
            Expediente expedienteDelDocumento = documento.getExpediente();

            if (!expedienteDelDocumento
                    .getOwner()
                    .getUserId()
                    .equals(userId)) {
                return false;
            }

            if (expediente != null
                    && !expediente.getCaseId().equals(
                    expedienteDelDocumento.getCaseId()
            )) {
                return false;
            }
        }

        return true;
    }

    private SummariesResponseDTO convertirAResponse(Summaries summary) {
        SummariesResponseDTO response = new SummariesResponseDTO();

        response.setSummaryId(summary.getSummaryId());
        response.setGeneratedByUserId(
                summary.getGeneratedBy().getUserId()
        );

        if (summary.getDocumento() != null) {
            response.setDocumentId(
                    summary.getDocumento().getDocumentId()
            );
        }

        if (summary.getExpediente() != null) {
            response.setCaseId(
                    summary.getExpediente().getCaseId()
            );
        }

        response.setSummaryType(summary.getSummaryType());
        response.setContent(summary.getContent());
        response.setCreatedAt(summary.getCreatedAt());

        return response;
    }
}