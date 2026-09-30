package pe.edu.upc.legalai.dtos.chatsession;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos de una sesión de chat")
public class ChatSessionResponseDTO {

    @Schema(description = "Identificador de la sesión", example = "1")
    private Long sessionId;

    @Schema(description = "Identificador del expediente asociado", example = "1")
    private Long caseId;

    @Schema(description = "Identificador del usuario dueño de la sesión", example = "1")
    private Long userId;

    @Schema(description = "Título de la sesión", example = "Consulta sobre contrato de arrendamiento")
    private String title;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    public ChatSessionResponseDTO() {
    }

    public ChatSessionResponseDTO(Long sessionId, Long caseId, Long userId, String title,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.userId = userId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

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
