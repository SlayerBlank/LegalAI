package pe.edu.upc.legalai.dtos.response;

public record ChunkGenerationResponseDTO(Long documentId, int chunksCreated, int chunkSize, int overlap) { }
