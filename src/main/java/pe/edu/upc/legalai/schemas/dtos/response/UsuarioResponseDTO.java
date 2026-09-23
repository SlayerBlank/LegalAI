package pe.edu.upc.legalai.schemas.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos publicos de un usuario")
public class UsuarioResponseDTO {

    private Long userId;
    private Long roleId;
    private String roleName;
    private String fullName;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UsuarioResponseDTO(Long userId, Long roleId, String roleName, String fullName, String email,
                              String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
