package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.entities.Usuario;

public interface AuditLogService {

    void registrar(Usuario usuario, String action, String entityType, Long entityId, String details);
}
