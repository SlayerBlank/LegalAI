package pe.edu.upc.legalai.dtos.draft;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.DraftStatus;

import java.time.LocalDateTime;

@Schema(description = "Datos de un borrador jurídico")
public class DraftResponseDTO {

    @Schema(description = "Identificador del borrador", example = "1")
    private Long draftId;

    @Schema(description = "Identificador del expediente asociado", example = "1")
    private Long caseId;

    @Schema(description = "Identificador del usuario que creó el borrador", example = "1")
    private Long createdBy;

    @Schema(description = "Título del borrador", example = "Contrato de arrendamiento - versión inicial")
    private String title;

    @Schema(description = "Instrucción o prompt utilizado para generar el borrador")
    private String prompt;

    @Schema(description = "Contenido del borrador")
    private String content;

    @Schema(description = "Estado del borrador", example = "DRAFT")
    private DraftStatus status;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    public DraftResponseDTO() {
    }

    public DraftResponseDTO(Long draftId, Long caseId, Long createdBy, String title, String prompt,
                            String content, DraftStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.draftId = draftId;
        this.caseId = caseId;
        this.createdBy = createdBy;
        this.title = title;
        this.prompt = prompt;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getDraftId() {
        return draftId;
    }

    public void setDraftId(Long draftId) {
        this.draftId = draftId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
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
