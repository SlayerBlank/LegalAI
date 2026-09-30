package pe.edu.upc.legalai.dtos.chatsession;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para crear o actualizar una sesión de chat")
public class ChatSessionRequestDTO {

    @Schema(description = "Identificador del expediente asociado", example = "1")
    @NotNull(message = "El expediente es obligatorio")
    private Long caseId;

    @Schema(description = "Identificador del usuario dueño de la sesión", example = "1")
    @NotNull(message = "El usuario es obligatorio")
    private Long userId;

    @Schema(description = "Título de la sesión", example = "Consulta sobre contrato de arrendamiento")
    @Size(max = 200, message = "El título no debe superar los 200 caracteres")
    private String title;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
