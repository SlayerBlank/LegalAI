package pe.edu.upc.legalai.DTOs.request;

import jakarta.validation.constraints.Size;

public class SesionChatRequestDTO {

    @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
    private String titulo;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
}