package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.auditlog.AuditLogRequestDTO;
import pe.edu.upc.legalai.dtos.auditlog.AuditLogResponseDTO;

import java.util.List;

public interface AuditLogService {

    AuditLogResponseDTO create(AuditLogRequestDTO request);

    List<AuditLogResponseDTO> findAll();

    AuditLogResponseDTO findById(Long id);

    List<AuditLogResponseDTO> findByUserId(Long userId);
}
