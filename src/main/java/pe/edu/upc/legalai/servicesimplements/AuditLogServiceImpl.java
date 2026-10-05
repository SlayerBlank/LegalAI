package pe.edu.upc.legalai.servicesimplements;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final IAuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(IAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Usuario usuario, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsuario(usuario);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    @Override
    public Page<AuditLogResponseDTO> buscar(Long userId, String action, String entityType, LocalDateTime from, LocalDateTime to, Pageable pageable) {
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

    // HU-070 - Query académica: auditoría mediante filtros combinados
    @Override
    public Page<AuditLogResponseDTO> buscarConsultaAcademica(
            Long userId, String action, String entityType, LocalDate from, LocalDate to,
            Pageable pageable) {
        requireAdmin();
        int filterCount = (userId == null ? 0 : 1)
                + (action == null || action.isBlank() ? 0 : 1)
                + (entityType == null || entityType.isBlank() ? 0 : 1)
                + (from == null ? 0 : 1)
                + (to == null ? 0 : 1);
        if (filterCount < 2) {
            throw new BadRequestException("Proporcione al menos dos filtros entre userId, action, entityType, from y to");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("La fecha from no puede ser posterior a to");
        }

        Pageable deterministicOrder = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("logId")));
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        return buscar(userId, action, entityType, fromDateTime, toDateTime, deterministicOrder);
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
