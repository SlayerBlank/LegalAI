package pe.edu.upc.legalai.schemas.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un rol")
public class RolResponseDTO {

    private Long roleId;
    private String name;
    private String description;

    public RolResponseDTO(Long roleId, String name, String description) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
