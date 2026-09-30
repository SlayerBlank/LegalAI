package pe.edu.upc.legalai.dtos.auditlog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para registrar una entrada de auditoría")
public class AuditLogRequestDTO {

    @Schema(description = "Identificador del usuario que realizó la acción", example = "1")
    @NotNull(message = "El usuario es obligatorio")
    private Long userId;

    @Schema(description = "Acción realizada", example = "CREATE")
    @NotBlank(message = "La acción es obligatoria")
    @Size(max = 100, message = "La acción no debe superar los 100 caracteres")
    private String action;

    @Schema(description = "Tipo de entidad afectada", example = "CaseFile")
    @NotBlank(message = "El tipo de entidad es obligatorio")
    @Size(max = 100, message = "El tipo de entidad no debe superar los 100 caracteres")
    private String entityType;

    @Schema(description = "Identificador de la entidad afectada", example = "1")
    @NotNull(message = "El identificador de la entidad es obligatorio")
    @Positive(message = "El identificador de la entidad debe ser mayor a cero")
    private Long entityId;

    @Schema(description = "Detalles adicionales de la acción", example = "Expediente creado desde el panel de clientes")
    private String details;

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
}
