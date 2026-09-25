package pe.edu.upc.legalai.DTOs.response;
import java.time.LocalDateTime;

public class SummariesResponseDTO {

    public Long getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(Long summaryId) {
        this.summaryId = summaryId;
    }

    public Long getDocumento_id() {
        return documento_id;
    }

    public void setDocumento_id(Long documento_id) {
        this.documento_id = documento_id;
    }

    public Long getExpediente_id() {
        return expediente_id;
    }

    public void setExpediente_id(Long expediente_id) {
        this.expediente_id = expediente_id;
    }

    public Long getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(Long usuario_id) {
        this.usuario_id = usuario_id;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private Long summaryId;
    private Long documento_id;
    private Long expediente_id;
    private Long usuario_id;
    private String summaryType;
    private String content;
    private LocalDateTime createdAt;

}