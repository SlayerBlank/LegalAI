package pe.edu.upc.legalai.dtos.role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un rol")
public class RoleResponseDTO {

    @Schema(description = "Identificador del rol", example = "1")
    private Long roleId;

    @Schema(description = "Nombre del rol", example = "LAWYER")
    private String name;

    @Schema(description = "Descripción del rol", example = "Profesional del derecho")
    private String description;

    public RoleResponseDTO() {
    }

    public RoleResponseDTO(Long roleId, String name, String description) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

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
