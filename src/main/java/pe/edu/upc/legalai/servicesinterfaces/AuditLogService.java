package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.entities.Usuario;

import java.util.List;

public interface AuditLogService {

    void registrar(Usuario usuario, String action, String entityType, Long entityId, String details);

    List<AuditLogResponseDTO> listar();

    AuditLogResponseDTO obtenerPorId(Long id);

    List<AuditLogResponseDTO> listarPorUsuario(Long userId);
}
