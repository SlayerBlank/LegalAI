package pe.edu.upc.legalai.DTOs.response;

public record EmbeddingGenerationResponseDTO(Long documentId, int chunksProcessed, int embeddingsGenerated) { }
