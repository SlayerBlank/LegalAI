package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.AuditLog;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.repositories.AuditLogRepository;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
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
}
