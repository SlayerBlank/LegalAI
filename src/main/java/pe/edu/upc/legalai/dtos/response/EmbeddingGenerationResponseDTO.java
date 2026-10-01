package pe.edu.upc.legalai.dtos.response;

public record EmbeddingGenerationResponseDTO(Long documentId, int chunksProcessed, int embeddingsGenerated) { }
