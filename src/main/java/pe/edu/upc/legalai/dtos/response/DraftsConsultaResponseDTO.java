package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resultado de la consulta de borradores por filtros")
public class DraftsConsultaResponseDTO {

    private Long draftId;
    private String title;
    private String status;
    private LocalDateTime updatedAt;

    public DraftsConsultaResponseDTO(
            Long draftId,
            String title,
            String status,
            LocalDateTime updatedAt
    ) {
        this.draftId = draftId;
        this.title = title;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Long getDraftId() {
        return draftId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}