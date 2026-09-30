package pe.edu.upc.legalai.dtos.draft;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.dtos.validation.OnCreate;
import pe.edu.upc.legalai.entities.DraftStatus;

@Schema(description = "Datos requeridos para crear o actualizar un borrador jurídico")
public class DraftRequestDTO {

    @Schema(description = "Identificador del expediente asociado", example = "1")
    @NotNull(groups = OnCreate.class, message = "El expediente es obligatorio")
    private Long caseId;

    @Schema(description = "Identificador del usuario que crea el borrador", example = "1")
    @NotNull(groups = OnCreate.class, message = "El usuario creador es obligatorio")
    private Long createdBy;

    @Schema(description = "Título del borrador", example = "Contrato de arrendamiento - versión inicial")
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no debe superar los 200 caracteres")
    private String title;

    @Schema(description = "Instrucción o prompt utilizado para generar el borrador",
            example = "Genera un contrato de arrendamiento de vivienda por un año")
    private String prompt;

    @Schema(description = "Contenido del borrador")
    private String content;

    @Schema(description = "Estado del borrador", example = "DRAFT")
    private DraftStatus status;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
    }
}
