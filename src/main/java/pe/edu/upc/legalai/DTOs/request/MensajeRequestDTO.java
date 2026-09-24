package pe.edu.upc.legalai.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MensajeRequestDTO {

    @NotBlank(message = "El tipo de remitente es obligatorio")
    @Size(max = 20, message = "El tipo de remitente no debe superar los 20 caracteres")
    private String senderType;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
