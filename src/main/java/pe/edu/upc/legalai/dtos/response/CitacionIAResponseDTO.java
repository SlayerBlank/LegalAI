package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Referencia persistida al fragmento utilizado, no una cita juridica verificada. "
        + "relevanceScore = 1 - distancia coseno; no representa confianza. "
        + "documentId puede ser null si el documento fue eliminado. No se inventan paginas.")
public record CitacionIAResponseDTO(Long citationId, Long messageId, Long documentId,
                                    Long chunkId, Double relevanceScore) { }
