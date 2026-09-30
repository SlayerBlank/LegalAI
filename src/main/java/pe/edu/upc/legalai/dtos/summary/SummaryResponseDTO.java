package pe.edu.upc.legalai.dtos.summary;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.SummaryType;

import java.time.LocalDateTime;

@Schema(description = "Datos de un resumen generado por IA")
public class SummaryResponseDTO {

    @Schema(description = "Identificador del resumen", example = "1")
    private Long summaryId;

    @Schema(description = "Identificador del documento resumido", example = "1")
    private Long documentId;

    @Schema(description = "Identificador del expediente resumido", example = "1")
    private Long caseId;

    @Schema(description = "Identificador del usuario que generó el resumen", example = "1")
    private Long generatedBy;

    @Schema(description = "Tipo de resumen", example = "DOCUMENT")
    private SummaryType summaryType;

    @Schema(description = "Contenido del resumen generado")
    private String content;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    public SummaryResponseDTO() {
    }

    public SummaryResponseDTO(Long summaryId, Long documentId, Long caseId, Long generatedBy,
                              SummaryType summaryType, String content, LocalDateTime createdAt) {
        this.summaryId = summaryId;
        this.documentId = documentId;
        this.caseId = caseId;
        this.generatedBy = generatedBy;
        this.summaryType = summaryType;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(Long summaryId) {
        this.summaryId = summaryId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
