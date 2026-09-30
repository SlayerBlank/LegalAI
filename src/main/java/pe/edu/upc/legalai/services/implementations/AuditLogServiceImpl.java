package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.auditlog.AuditLogRequestDTO;
import pe.edu.upc.legalai.dtos.auditlog.AuditLogResponseDTO;
import pe.edu.upc.legalai.entities.AuditLog;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.AuditLogRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.AuditLogService;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AuditLogResponseDTO create(AuditLogRequestDTO request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + request.getUserId()));
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(request.getAction());
        log.setEntityType(request.getEntityType());
        log.setEntityId(request.getEntityId());
        log.setDetails(request.getDetails());
        return toResponse(auditLogRepository.save(log));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> findAll() {
        return auditLogRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponseDTO findById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado con id: " + id));
        return toResponse(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> findByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }
        return auditLogRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponseDTO toResponse(AuditLog log) {
        return new AuditLogResponseDTO(
                log.getLogId(),
                log.getUser().getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
