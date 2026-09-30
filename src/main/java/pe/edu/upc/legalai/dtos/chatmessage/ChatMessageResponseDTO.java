package pe.edu.upc.legalai.dtos.chatmessage;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.SenderType;

import java.time.LocalDateTime;

@Schema(description = "Datos de un mensaje de chat")
public class ChatMessageResponseDTO {

    @Schema(description = "Identificador del mensaje", example = "1")
    private Long messageId;

    @Schema(description = "Identificador de la sesión de chat", example = "1")
    private Long sessionId;

    @Schema(description = "Tipo de emisor del mensaje", example = "USER")
    private SenderType senderType;

    @Schema(description = "Contenido del mensaje")
    private String content;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    public ChatMessageResponseDTO() {
    }

    public ChatMessageResponseDTO(Long messageId, Long sessionId, SenderType senderType, String content,
                                  LocalDateTime createdAt) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.senderType = senderType;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public SenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(SenderType senderType) {
        this.senderType = senderType;
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
