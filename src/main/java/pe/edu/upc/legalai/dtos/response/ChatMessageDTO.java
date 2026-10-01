package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Mensaje persistido; las fuentes son fragmentos consultados, no citas verificadas por afirmacion")
public record ChatMessageDTO(
        @Schema(example = "101") Long messageId,
        @Schema(description = "USER o ASSISTANT. El cliente nunca lo envia", example = "USER") String role,
        String content,
        LocalDateTime createdAt,
        @Schema(description = "Null si no se conservaron metadatos de la respuesta") String provider,
        @Schema(description = "Null si no se conservaron metadatos de la respuesta") String model,
        @Schema(description = "Null si no se conservaron metadatos de la respuesta") Integer retrievedChunks,
        @Schema(description = "CONSULTED_FRAGMENTS; null si los metadatos no estan disponibles") String sourcesType,
        @Schema(description = "Fuentes consultadas, no citas verificadas por afirmacion; null si no hay metadatos")
        List<RAGSourceDTO> sources) {

    public ChatMessageDTO(Long messageId, String role, String content, LocalDateTime createdAt) {
        this(messageId, role, content, createdAt, null, null, null, null, null);
    }
}