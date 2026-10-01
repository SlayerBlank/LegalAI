package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Sesion de chat persistida. El alcance documental lo fija el backend y no puede cambiarse desde el cliente")
public record ChatSessionResponseDTO(
        @Schema(example = "12") Long sessionId,
        @Schema(example = "1") Long caseId,
        @Schema(description = "Documento que acota la sesion, o null si usa el expediente completo", example = "5") Long documentId,
        @Schema(description = "True si la sesion fue limitada a un documento; si documentId es null, el documento fue eliminado y no se amplia el alcance")
        boolean documentScopeRequired,
        String title,
        @Schema(description = "Numero de mensajes registrados en la sesion", example = "4") long messageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ChatSessionResponseDTO(Long sessionId, Long caseId, Long documentId, String title, long messageCount,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(sessionId, caseId, documentId, documentId != null, title, messageCount, createdAt, updatedAt);
    }
}