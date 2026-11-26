package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;

import java.time.LocalDateTime;

/**
 * HU-67: documento pendiente de revision. Conserva todos los campos de DocumentoResponseDTO
 * (el frontend ya los usa) y agrega el titulo del expediente al que pertenece.
 */
@Schema(description = "Documento pendiente de revision con el titulo de su expediente")
public class DocumentoPendienteResponseDTO extends DocumentoResponseDTO {

    private final String caseTitle;

    public DocumentoPendienteResponseDTO(Long documentId, Long caseId, String caseTitle, String fileName,
                                         String fileType, String storageUrl, String category, Long sizeBytes,
                                         EstadoProcesamiento processingStatus, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        super(documentId, caseId, fileName, fileType, storageUrl, category, sizeBytes, processingStatus,
                createdAt, updatedAt);
        this.caseTitle = caseTitle;
    }

    public String getCaseTitle() {
        return caseTitle;
    }
}
