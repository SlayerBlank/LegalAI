package pe.edu.upc.legalai.dtos.auditlog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos de una entrada de auditoría")
public class AuditLogResponseDTO {

    @Schema(description = "Identificador del registro de auditoría", example = "1")
    private Long logId;

    @Schema(description = "Identificador del usuario que realizó la acción", example = "1")
    private Long userId;

    @Schema(description = "Acción realizada", example = "CREATE")
    private String action;

    @Schema(description = "Tipo de entidad afectada", example = "CaseFile")
    private String entityType;

    @Schema(description = "Identificador de la entidad afectada", example = "1")
    private Long entityId;

    @Schema(description = "Detalles adicionales de la acción")
    private String details;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    public AuditLogResponseDTO() {
    }

    public AuditLogResponseDTO(Long logId, Long userId, String action, String entityType, Long entityId,
                               String details, LocalDateTime createdAt) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
