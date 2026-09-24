package pe.edu.upc.legalai.DTOs.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.EstadoExpediente;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Datos de un expediente")
public class ExpedienteResponseDTO {

    private Long caseId;
    private Long clientId;
    private String title;
    private String description;
    private EstadoExpediente status;
    private LocalDate openedAt;
    private LocalDate closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ExpedienteResponseDTO(Long caseId, Long clientId, String title, String description,
                                 EstadoExpediente status, LocalDate openedAt, LocalDate closedAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.caseId = caseId;
        this.clientId = clientId;
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

    public Long getClientId() {
        return clientId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EstadoExpediente getStatus() {
        return status;
    }

    public LocalDate getOpenedAt() {
        return openedAt;
    }

    public LocalDate getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
