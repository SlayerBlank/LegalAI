package pe.edu.upc.legalai.DTOs.response;

public record DocumentChunkResponseDTO(Long chunkId, Long documentId, int chunkIndex,
                                      String content, int charStart, int charEnd) { }
