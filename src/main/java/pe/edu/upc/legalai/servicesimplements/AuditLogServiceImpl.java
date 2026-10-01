package pe.edu.upc.legalai.servicesimplements;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.entities.AuditLog;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.AuditLogSpecifications;
import pe.edu.upc.legalai.repositories.IAuditLogRepository;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final IAuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(IAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(
            Usuario usuario,
            String action,
            String entityType,
            Long entityId,
            String details
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsuario(usuario);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> buscar(
            Long userId,
            String action,
            String entityType,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        requireAdmin();

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(
                    "La fecha from no puede ser posterior a to"
            );
        }

        return auditLogRepository
                .findAll(
                        AuditLogSpecifications.conFiltros(
                                userId,
                                action,
                                entityType,
                                from,
                                to
                        ),
                        pageable
                )
                .map(this::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponseDTO obtenerPorId(Long id) {
        requireAdmin();

        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Registro de auditoría no encontrado"
                        )
                );

        return toResponseDTO(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> listarPorUsuario(Long userId) {
        requireAdmin();

        return auditLogRepository
                .findByUsuarioUserIdOrderByCreatedAtDescLogIdDesc(userId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private AuditLogResponseDTO toResponseDTO(AuditLog auditLog) {
        return new AuditLogResponseDTO(
                auditLog.getLogId(),
                auditLog.getUsuario().getUserId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }

    private void requireAdmin() {
        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities()
                .stream()
                .noneMatch(a ->
                        "ROLE_ADMIN".equals(a.getAuthority()))) {

            throw new AccessDeniedException(
                    "La consulta de auditoría requiere ROLE_ADMIN"
            );
        }
    }
}