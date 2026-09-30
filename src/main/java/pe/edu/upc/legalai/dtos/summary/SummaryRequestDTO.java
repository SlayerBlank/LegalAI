package pe.edu.upc.legalai.dtos.summary;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.upc.legalai.entities.SummaryType;

@Schema(description = "Datos requeridos para crear un resumen generado por IA")
public class SummaryRequestDTO {

    @Schema(description = "Identificador del documento resumido (obligatorio si el tipo es DOCUMENT)", example = "1")
    private Long documentId;

    @Schema(description = "Identificador del expediente resumido (obligatorio si el tipo es CASE)", example = "1")
    private Long caseId;

    @Schema(description = "Identificador del usuario que generó el resumen", example = "1")
    @NotNull(message = "El usuario que genera el resumen es obligatorio")
    private Long generatedBy;

    @Schema(description = "Tipo de resumen", example = "DOCUMENT")
    @NotNull(message = "El tipo de resumen es obligatorio")
    private SummaryType summaryType;

    @Schema(description = "Contenido del resumen generado")
    @NotBlank(message = "El contenido del resumen es obligatorio")
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

    public Long getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(Long generatedBy) {
        this.generatedBy = generatedBy;
    }

    public SummaryType getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(SummaryType summaryType) {
        this.summaryType = summaryType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
