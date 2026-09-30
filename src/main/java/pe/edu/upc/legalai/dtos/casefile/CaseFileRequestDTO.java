package pe.edu.upc.legalai.dtos.casefile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.entities.CaseFileStatus;

@Schema(description = "Datos requeridos para crear o actualizar un expediente")
public class CaseFileRequestDTO {

    @Schema(description = "Identificador del cliente asociado", example = "1")
    @NotNull(message = "El cliente es obligatorio")
    private Long clientId;

    @Schema(description = "Identificador del usuario propietario del expediente", example = "1")
    @NotNull(message = "El usuario propietario es obligatorio")
    private Long ownerUserId;

    @Schema(description = "Título del expediente", example = "Demanda laboral - Empresa XYZ")
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no debe superar los 200 caracteres")
    private String title;

    @Schema(description = "Descripción del expediente", example = "Expediente relacionado a despido injustificado")
    private String description;

    @Schema(description = "Estado del expediente", example = "OPEN")
    private CaseFileStatus status;

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
}
