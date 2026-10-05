package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.request.DraftsRequestDTO;
import pe.edu.upc.legalai.dtos.response.DraftsResponseDTO;
import pe.edu.upc.legalai.entities.Drafts;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDraftsRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IDraftsService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;

@Service
public class DraftsServiceImplement implements IDraftsService {

    private final IDraftsRepository draftsRepository;
    private final IExpedienteRepository expedienteRepository;
    private final IUsuarioService usuarioService;
    private final AuditLogService auditLogService;

    public DraftsServiceImplement(
            IDraftsRepository draftsRepository,
            IExpedienteRepository expedienteRepository,
            IUsuarioService usuarioService,
            AuditLogService auditLogService
    ) {
        this.draftsRepository = draftsRepository;
        this.expedienteRepository = expedienteRepository;
        this.usuarioService = usuarioService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public DraftsResponseDTO registrar(DraftsRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Drafts draft = new Drafts();
        aplicarDatos(draft, request, usuario.getUserId());
        draft.setCreatedBy(usuario);

        Drafts guardado = draftsRepository.saveAndFlush(draft);

        auditLogService.registrar(
                usuario,
                "CREATE_DRAFT",
                "Drafts",
                guardado.getDraftId(),
                "Borrador creado"
        );

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftsResponseDTO> listarPorUsuarioAutenticado() {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        return draftsRepository
                .findByCreatedByUserIdAndExpedienteOwnerUserIdOrderByDraftIdDesc(
                        usuario.getUserId(),
                        usuario.getUserId()
                )
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DraftsResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Drafts draft = obtenerBorradorPropio(id, usuario.getUserId());

        return convertirAResponse(draft);
    }

    @Override
    @Transactional
    public DraftsResponseDTO actualizar(
            Long id,
            DraftsRequestDTO request
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Drafts draft = obtenerBorradorPropio(id, usuario.getUserId());
        aplicarDatos(draft, request, usuario.getUserId());

        Drafts guardado = draftsRepository.saveAndFlush(draft);

        auditLogService.registrar(
                usuario,
                "UPDATE_DRAFT",
                "Drafts",
                guardado.getDraftId(),
                "Borrador actualizado"
        );

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Drafts draft = obtenerBorradorPropio(id, usuario.getUserId());

        auditLogService.registrar(
                usuario,
                "DELETE_DRAFT",
                "Drafts",
                draft.getDraftId(),
                "Borrador eliminado"
        );

        draftsRepository.delete(draft);
    }

    private Drafts obtenerBorradorPropio(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BadRequestException(
                    "El ID del borrador debe ser positivo"
            );
        }

        return draftsRepository
                .findByDraftIdAndCreatedByUserIdAndExpedienteOwnerUserId(
                        id,
                        userId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Borrador no encontrado"
                ));
    }

    private Expediente obtenerExpedientePropio(
            Long caseId,
            Long userId
    ) {
        return expedienteRepository
                .findByCaseIdAndOwnerUserId(caseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expediente no encontrado"
                ));
    }

    private void aplicarDatos(
            Drafts draft,
            DraftsRequestDTO request,
            Long userId
    ) {
        Expediente expediente = obtenerExpedientePropio(
                request.getCaseId(),
                userId
        );

        draft.setExpediente(expediente);
        draft.setTitle(request.getTitle());
        draft.setPrompt(request.getPrompt());
        draft.setContent(request.getContent());
        draft.setStatus(request.getStatus());
    }

    private DraftsResponseDTO convertirAResponse(Drafts draft) {
        DraftsResponseDTO response = new DraftsResponseDTO();

        response.setDraftId(draft.getDraftId());
        response.setCaseId(draft.getExpediente().getCaseId());
        response.setCreatedByUserId(draft.getCreatedBy().getUserId());
        response.setTitle(draft.getTitle());
        response.setPrompt(draft.getPrompt());
        response.setContent(draft.getContent());
        response.setStatus(draft.getStatus());
        response.setCreatedAt(draft.getCreatedAt());
        response.setUpdatedAt(draft.getUpdatedAt());

        return response;
    }
}