package pe.edu.upc.legalai.DTOs.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatSendMessageRequestDTO {

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    @Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
    @Schema(description = "Pregunta del usuario. El rol lo decide el backend", example = "¿Cuales son las obligaciones principales?")
    private String content;

    @Size(max = 64, message = "clientMessageId no puede superar 64 caracteres")
    @Schema(description = "Identificador opcional generado por el cliente. Repetirlo devuelve el turno ya registrado "
            + "en lugar de generar una respuesta nueva", example = "uuid-del-cliente")
    private String clientMessageId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getClientMessageId() { return clientMessageId; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Solo se permiten content y clientMessageId");
    }
}