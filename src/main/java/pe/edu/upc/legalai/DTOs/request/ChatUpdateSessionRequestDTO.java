package pe.edu.upc.legalai.DTOs.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatUpdateSessionRequestDTO {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
    @Schema(description = "Nuevo titulo de la conversacion", example = "Analisis del contrato")
    private String title;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Solo se permite title");
    }
}