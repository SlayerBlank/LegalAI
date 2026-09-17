package pe.edu.upc.legalai.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.dtos.validation.OnCreate;

@Schema(description = "Datos requeridos para crear o actualizar un usuario")
public class UserRequestDTO {

    @Schema(description = "Identificador del rol asignado al usuario", example = "1")
    @NotNull(message = "El rol es obligatorio")
    private Long roleId;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre completo no debe superar los 150 caracteres")
    private String fullName;

    @Schema(description = "Correo electrónico único del usuario", example = "juan@legalai.com")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 150, message = "El correo electrónico no debe superar los 150 caracteres")
    private String email;

    @Schema(description = "Contraseña del usuario (obligatoria al crear)", example = "123456")
    @NotBlank(groups = OnCreate.class, message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    @Schema(description = "Indica si el usuario está activo. Si no se envía, se asigna true", example = "true")
    private Boolean active;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
