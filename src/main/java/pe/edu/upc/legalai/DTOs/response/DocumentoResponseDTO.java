package pe.edu.upc.legalai.DTOs.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;

import java.time.LocalDateTime;

@Schema(description = "Metadatos de un documento")
public class DocumentoResponseDTO {

    private Long documentId;
    private Long caseId;
    private String fileName;
    private String fileType;
    private String storageUrl;
    private String category;
    private Long sizeBytes;
    private EstadoProcesamiento processingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentoResponseDTO(Long documentId, Long caseId, String fileName, String fileType, String storageUrl,
                                String category, Long sizeBytes, EstadoProcesamiento processingStatus,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.documentId = documentId;
        this.caseId = caseId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.storageUrl = storageUrl;
        this.category = category;
        this.sizeBytes = sizeBytes;
        this.processingStatus = processingStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public String getCategory() {
        return category;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public EstadoProcesamiento getProcessingStatus() {
        return processingStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
