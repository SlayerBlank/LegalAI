package pe.edu.upc.legalai.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos de un usuario (sin exponer credenciales)")
public class UserResponseDTO {

    @Schema(description = "Identificador del usuario", example = "1")
    private Long userId;

    @Schema(description = "Identificador del rol asignado", example = "1")
    private Long roleId;

    @Schema(description = "Nombre del rol asignado", example = "LAWYER")
    private String roleName;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String fullName;

    @Schema(description = "Correo electrónico del usuario", example = "juan@legalai.com")
    private String email;

    @Schema(description = "Indica si el usuario está activo", example = "true")
    private boolean active;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    public UserResponseDTO() {
    }

    public UserResponseDTO(Long userId, Long roleId, String roleName, String fullName, String email,
                           boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
