package pe.edu.upc.legalai.DTOs.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CitacionIARequestDTO {

    @NotNull(message = "El mensaje es obligatorio")
    @Positive(message = "El ID del mensaje debe ser positivo")
    private Long messageId;

    @Positive(message = "El ID del documento debe ser positivo")
    private Long documentId;

    @Positive(message = "El ID del fragmento debe ser positivo")
    private Long chunkId;

    @DecimalMin(value = "0.0", message = "La relevancia no puede ser menor que 0")
    @DecimalMax(value = "1.0", message = "La relevancia no puede ser mayor que 1")
    private Double relevanceScore;

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
}
