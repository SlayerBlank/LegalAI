package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resultado de la consulta de resúmenes por filtros")
public class SummariesConsultaResponseDTO {

    private Long summaryId;
    private Long documentId;
    private String summaryType;
    private String content;
    private LocalDateTime createdAt;

    public SummariesConsultaResponseDTO(
            Long summaryId,
            Long documentId,
            String summaryType,
            String content,
            LocalDateTime createdAt
    ) {
        this.summaryId = summaryId;
        this.documentId = documentId;
        this.summaryType = summaryType;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getSummaryId() {
        return summaryId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}