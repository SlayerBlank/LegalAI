package pe.edu.upc.legalai.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Edicion administrativa del perfil. El correo identifica al usuario en JWT y debe conservarse. No admite rol, estado ni credenciales.")
public record UsuarioUpdateRequestDTO(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150)
        @Schema(description = "Correo actual; no se permite cambiar la identidad JWT") String email) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Solo se permiten fullName y email");
    }
}

