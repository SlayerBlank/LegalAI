package pe.edu.upc.legalai.DTOs.response;

public record ChunkGenerationResponseDTO(Long documentId, int chunksCreated, int chunkSize, int overlap) { }
