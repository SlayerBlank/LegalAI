package pe.edu.upc.legalai.dtos.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.entities.DocumentCategory;

@Schema(description = "Datos requeridos para crear o actualizar un documento")
public class DocumentRequestDTO {

    @Schema(description = "Identificador del usuario que sube el documento", example = "1")
    @NotNull(message = "El usuario que sube el documento es obligatorio")
    private Long uploadedByUserId;

    @Schema(description = "Nombre del archivo", example = "contrato.pdf")
    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Size(max = 255, message = "El nombre del archivo no debe superar los 255 caracteres")
    private String fileName;

    @Schema(description = "Tipo MIME del archivo", example = "application/pdf")
    @NotBlank(message = "El tipo de archivo es obligatorio")
    @Size(max = 100, message = "El tipo de archivo no debe superar los 100 caracteres")
    private String fileType;

    @Schema(description = "URL o ruta de almacenamiento del archivo", example = "/documents/contrato.pdf")
    @Size(max = 500, message = "La URL de almacenamiento no debe superar los 500 caracteres")
    private String storageUrl;

    @Schema(description = "Categoría del documento", example = "CONTRACT")
    private DocumentCategory category;

    @Schema(description = "Tamaño del archivo en bytes", example = "245000")
    @PositiveOrZero(message = "El tamaño del archivo no puede ser negativo")
    private Long sizeBytes;

    @Schema(description = "Texto extraído del documento", example = "null")
    private String extractedText;

    @Schema(description = "Indica si el documento ya fue procesado. Si no se envía, se asigna false", example = "false")
    private Boolean processed;

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

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }
}
