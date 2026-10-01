package pe.edu.upc.legalai.dtos.response;

import java.time.LocalDateTime;

public record AuditLogResponseDTO(Long logId, Long userId, String action, String entityType,
                                  Long entityId, String details, LocalDateTime createdAt) { }
