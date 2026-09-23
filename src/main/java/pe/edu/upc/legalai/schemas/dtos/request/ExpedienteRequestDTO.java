package pe.edu.upc.legalai.schemas.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.entities.EstadoExpediente;

import java.time.LocalDate;

@Schema(description = "Datos para crear o actualizar un expediente")
public class ExpedienteRequestDTO {

    @NotNull(message = "El cliente es obligatorio")
    @Positive(message = "El identificador del cliente debe ser positivo")
    private Long clientId;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 200, message = "El titulo no debe superar los 200 caracteres")
    private String title;

    private String description;

    @Schema(description = "Al crear se utiliza OPEN si se omite; al actualizar se conserva el estado actual")
    private EstadoExpediente status;

    @Schema(description = "Al crear se utiliza la fecha actual si se omite; al actualizar se conserva", example = "2026-09-23")
    private LocalDate openedAt;

    @Schema(description = "Solo para CLOSED o ARCHIVED; al cerrar se completa automaticamente y al reabrir se elimina")
    private LocalDate closedAt;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public EstadoExpediente getStatus() {
        return status;
    }

    public void setStatus(EstadoExpediente status) {
        this.status = status;
    }

    public LocalDate getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDate openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDate getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDate closedAt) {
        this.closedAt = closedAt;
    }
}
