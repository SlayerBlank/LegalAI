package pe.edu.upc.legalai.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar un borrador")
public class DraftsRequestDTO {

    @NotNull(message = "El expediente es obligatorio")
    @Positive(message = "El ID del expediente debe ser positivo")
    private Long caseId;

    @NotBlank(message = "El título es obligatorio")
    @Size(
            max = 255,
            message = "El título no debe superar los 255 caracteres"
    )
    private String title;

    private String prompt;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    @NotBlank(message = "El estado es obligatorio")
    @Size(
            max = 50,
            message = "El estado no debe superar los 50 caracteres"
    )
    private String status;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}