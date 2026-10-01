package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

@Service
public class ChatAuditService {

    private final AuditLogService audit;

    public ChatAuditService(AuditLogService audit) {
        this.audit = audit;
    }

    @Transactional
    public void registrar(Usuario usuario, String action, Long sessionId, String details) {
        audit.registrar(usuario, action, "ChatSession", sessionId, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Usuario usuario, String action, Long sessionId, String details) {
        audit.registrar(usuario, action, "ChatSession", sessionId, details);
    }
}