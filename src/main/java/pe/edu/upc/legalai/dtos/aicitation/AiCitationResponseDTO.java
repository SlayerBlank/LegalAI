package pe.edu.upc.legalai.dtos.aicitation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos de una cita generada por la IA")
public class AiCitationResponseDTO {

    @Schema(description = "Identificador de la cita", example = "1")
    private Long citationId;

    @Schema(description = "Identificador del mensaje de chat que origina la cita", example = "1")
    private Long messageId;

    @Schema(description = "Identificador del documento citado", example = "1")
    private Long documentId;

    @Schema(description = "Identificador del fragmento de documento citado", example = "1")
    private Long chunkId;

    @Schema(description = "Puntaje de relevancia asignado a la cita", example = "0.87")
    private Double relevanceScore;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    public AiCitationResponseDTO() {
    }

    public AiCitationResponseDTO(Long citationId, Long messageId, Long documentId, Long chunkId,
                                 Double relevanceScore, LocalDateTime createdAt) {
        this.citationId = citationId;
        this.messageId = messageId;
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.relevanceScore = relevanceScore;
        this.createdAt = createdAt;
    }

    public Long getCitationId() {
        return citationId;
    }

    public void setCitationId(Long citationId) {
        this.citationId = citationId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
