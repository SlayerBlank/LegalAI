package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

@Service
public class RAGAuditService {
    private final AuditLogService audit;
    public RAGAuditService(AuditLogService audit) { this.audit = audit; }

    // Preserve a failed-query audit even after retrieval's transaction rolls back.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Usuario user, String scope, Long id, int chunks, boolean success) {
        audit.registrar(user, "RAG_QUERY", scope, id,
                "queryType=" + scope + ";retrievedChunks=" + chunks + ";result=" + (success ? "SUCCESS" : "FAILED"));
    }
}
