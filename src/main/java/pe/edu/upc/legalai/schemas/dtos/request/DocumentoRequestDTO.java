package pe.edu.upc.legalai.schemas.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Metadatos de un archivo almacenado externamente. No carga archivos ni realiza analisis")
public class DocumentoRequestDTO {

    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Size(max = 255, message = "El nombre del archivo no debe superar los 255 caracteres")
    private String fileName;

    @Size(max = 100, message = "El tipo de archivo no debe superar los 100 caracteres")
    private String fileType;

    @Size(max = 500, message = "La URL de almacenamiento no debe superar los 500 caracteres")
    private String storageUrl;

    @Size(max = 80, message = "La categoria no debe superar los 80 caracteres")
    private String category;

    @PositiveOrZero(message = "El tamano del archivo no puede ser negativo")
    private Long sizeBytes;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
