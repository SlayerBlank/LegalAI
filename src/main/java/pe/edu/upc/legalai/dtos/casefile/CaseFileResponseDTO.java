package pe.edu.upc.legalai.dtos.casefile;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.CaseFileStatus;

import java.time.LocalDateTime;

@Schema(description = "Datos de un expediente")
public class CaseFileResponseDTO {

    @Schema(description = "Identificador del expediente", example = "1")
    private Long caseId;

    @Schema(description = "Identificador del cliente asociado", example = "1")
    private Long clientId;

    @Schema(description = "Identificador del usuario propietario", example = "1")
    private Long ownerUserId;

    @Schema(description = "Título del expediente", example = "Demanda laboral - Empresa XYZ")
    private String title;

    @Schema(description = "Descripción del expediente")
    private String description;

    @Schema(description = "Estado del expediente", example = "OPEN")
    private CaseFileStatus status;

    @Schema(description = "Fecha de apertura")
    private LocalDateTime openedAt;

    @Schema(description = "Fecha de cierre")
    private LocalDateTime closedAt;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    public CaseFileResponseDTO() {
    }

    public CaseFileResponseDTO(Long caseId, Long clientId, Long ownerUserId, String title, String description,
                               CaseFileStatus status, LocalDateTime openedAt, LocalDateTime closedAt,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.caseId = caseId;
        this.clientId = clientId;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CaseFileStatus getStatus() {
        return status;
    }

    public void setStatus(CaseFileStatus status) {
        this.status = status;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
