package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;

/**
 * HU-028: texto extraido de un documento. Si aun no hay texto, {@code text} es null y
 * {@code processingStatus} y {@code message} explican por que.
 */
@Schema(description = "Texto extraido, estado de procesamiento y mensaje cuando el texto no esta disponible")
public record DocumentoTextResponseDTO(Long documentId, EstadoProcesamiento processingStatus, String text,
                                       String message) { }
