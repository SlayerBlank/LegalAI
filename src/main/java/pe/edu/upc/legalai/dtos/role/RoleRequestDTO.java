package pe.edu.upc.legalai.dtos.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para crear o actualizar un rol")
public class RoleRequestDTO {

    @Schema(description = "Nombre único del rol", example = "LAWYER")
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre no debe superar los 50 caracteres")
    private String name;

    @Schema(description = "Descripción del rol", example = "Profesional del derecho")
    @Size(max = 255, message = "La descripción no debe superar los 255 caracteres")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
