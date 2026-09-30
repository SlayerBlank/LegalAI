package pe.edu.upc.legalai.dtos.chatmessage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.upc.legalai.entities.SenderType;

@Schema(description = "Datos requeridos para crear un mensaje de chat")
public class ChatMessageRequestDTO {

    @Schema(description = "Identificador de la sesión de chat", example = "1")
    @NotNull(message = "La sesión de chat es obligatoria")
    private Long sessionId;

    @Schema(description = "Tipo de emisor del mensaje", example = "USER")
    @NotNull(message = "El tipo de emisor es obligatorio")
    private SenderType senderType;

    @Schema(description = "Contenido del mensaje", example = "¿Cuáles son las cláusulas de rescisión del contrato?")
    @NotBlank(message = "El contenido del mensaje es obligatorio")
    private String content;

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
}
