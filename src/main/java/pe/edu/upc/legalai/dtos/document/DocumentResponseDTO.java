package pe.edu.upc.legalai.dtos.document;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.DocumentCategory;

import java.time.LocalDateTime;

@Schema(description = "Datos de un documento")
public class DocumentResponseDTO {

    @Schema(description = "Identificador del documento", example = "1")
    private Long documentId;

    @Schema(description = "Identificador del usuario que subió el documento", example = "1")
    private Long uploadedByUserId;

    @Schema(description = "Nombre del archivo", example = "contrato.pdf")
    private String fileName;

    @Schema(description = "Tipo MIME del archivo", example = "application/pdf")
    private String fileType;

    @Schema(description = "URL o ruta de almacenamiento", example = "/documents/contrato.pdf")
    private String storageUrl;

    @Schema(description = "Categoría del documento", example = "CONTRACT")
    private DocumentCategory category;

    @Schema(description = "Tamaño del archivo en bytes", example = "245000")
    private Long sizeBytes;

    @Schema(description = "Texto extraído del documento")
    private String extractedText;

    @Schema(description = "Indica si el documento ya fue procesado", example = "false")
    private boolean processed;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    public DocumentResponseDTO() {
    }

    public DocumentResponseDTO(Long documentId, Long uploadedByUserId, String fileName, String fileType,
                               String storageUrl, DocumentCategory category, Long sizeBytes, String extractedText,
                               boolean processed, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.documentId = documentId;
        this.uploadedByUserId = uploadedByUserId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.storageUrl = storageUrl;
        this.category = category;
        this.sizeBytes = sizeBytes;
        this.extractedText = extractedText;
        this.processed = processed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(Long uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
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
