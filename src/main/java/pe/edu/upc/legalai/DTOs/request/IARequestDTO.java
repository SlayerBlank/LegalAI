package pe.edu.upc.legalai.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IARequestDTO {
    @NotBlank(message = "El prompt es obligatorio")
    @Size(max = 10000, message = "El prompt no puede superar 10000 caracteres")
    private String prompt;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
