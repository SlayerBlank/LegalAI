package pe.edu.upc.legalai.dtos.aicitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos requeridos para registrar una cita generada por la IA")
public class AiCitationRequestDTO {

    @Schema(description = "Identificador del mensaje de chat que origina la cita", example = "1")
    @NotNull(message = "El mensaje es obligatorio")
    private Long messageId;

    @Schema(description = "Identificador del documento citado", example = "1")
    @NotNull(message = "El documento es obligatorio")
    private Long documentId;

    @Schema(description = "Identificador del fragmento de documento citado", example = "1")
    @NotNull(message = "El fragmento de documento es obligatorio")
    private Long chunkId;

    @Schema(description = "Puntaje de relevancia asignado a la cita", example = "0.87")
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
