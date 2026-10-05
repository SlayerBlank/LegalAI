package pe.edu.upc.legalai.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar un resumen")
public class SummariesRequestDTO {

    @Positive(message = "El ID del documento debe ser positivo")
    private Long documentId;

    @Positive(message = "El ID del expediente debe ser positivo")
    private Long caseId;

    @NotBlank(message = "El tipo de resumen es obligatorio")
    @Size(
            max = 100,
            message = "El tipo de resumen no debe superar los 100 caracteres"
    )
    private String summaryType;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}