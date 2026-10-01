package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.AuditLog;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.repositories.IAuditLogRepository;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> buscar(Long userId, String action, String entityType,
                                           LocalDateTime from, LocalDateTime to, Pageable pageable) {
        requireAdmin();
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("La fecha from no puede ser posterior a to");
        }
        Specification<AuditLog> filters = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) predicates.add(cb.equal(root.get("usuario").get("userId"), userId));
            if (action != null && !action.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("action")), action.toLowerCase(Locale.ROOT)));
            if (entityType != null && !entityType.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("entityType")), entityType.toLowerCase(Locale.ROOT)));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return auditLogRepository.findAll(filters, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponseDTO obtenerPorId(Long id) {
        requireAdmin();
        return toResponse(auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoria no encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> listarPorUsuario(Long userId) {
        requireAdmin();
        return auditLogRepository.findByUsuarioUserIdOrderByCreatedAtDescLogIdDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponseDTO toResponse(AuditLog log) {
        return new AuditLogResponseDTO(log.getLogId(), log.getUsuario().getUserId(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getDetails(), log.getCreatedAt());
    }

    private void requireAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("La consulta de auditoria requiere ROLE_ADMIN");
        }
    }
}
