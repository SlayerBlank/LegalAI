package pe.edu.upc.legalai.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Rol normalizado a mayusculas, sin prefijo ROLE_. ADMIN y USER son nombres reservados.")
public record RolRequestDTO(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*")
        @Schema(example = "REVIEWER") String name,
        @Size(max = 255) String description) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Solo se permiten name y description");
    }
}

