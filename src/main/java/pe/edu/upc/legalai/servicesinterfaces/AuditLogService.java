package pe.edu.upc.legalai.servicesinterfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.upc.legalai.DTOs.response.AuditLogResponseDTO;
import pe.edu.upc.legalai.entities.Usuario;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

    void registrar(Usuario usuario, String action, String entityType, Long entityId, String details);

    Page<AuditLogResponseDTO> buscar(Long userId, String action, String entityType,
                                      LocalDateTime from, LocalDateTime to, Pageable pageable);

    AuditLogResponseDTO obtenerPorId(Long id);

    List<AuditLogResponseDTO> listarPorUsuario(Long userId);
}
