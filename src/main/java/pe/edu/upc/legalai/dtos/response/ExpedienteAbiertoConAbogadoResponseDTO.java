package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.EstadoExpediente;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Expediente abierto del usuario con datos del abogado propietario")
public class ExpedienteAbiertoConAbogadoResponseDTO {

    private Long caseId;
    private String title;
    private String description;
    private EstadoExpediente status;
    private LocalDate openedAt;
    private LocalDateTime updatedAt;
    private UsuarioResponseDTO abogado;

    public ExpedienteAbiertoConAbogadoResponseDTO(Long caseId, String title, String description,
                                                 EstadoExpediente status, LocalDate openedAt,
                                                 LocalDateTime updatedAt, UsuarioResponseDTO abogado) {
        this.caseId = caseId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.openedAt = openedAt;
        this.updatedAt = updatedAt;
        this.abogado = abogado;
    }

    public Long getCaseId() {
        return caseId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UsuarioResponseDTO getAbogado() {
        return abogado;
    }
}
