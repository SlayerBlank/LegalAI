package pe.edu.upc.legalai.DTOs.response;

public record SemanticSearchResultDTO(Long chunkId, Long documentId, int chunkIndex,
                                      String content, double distance, String documentName,
                                      Integer charStart, Integer charEnd) {
    public SemanticSearchResultDTO(Long chunkId, Long documentId, int chunkIndex, String content, double distance) {
        this(chunkId, documentId, chunkIndex, content, distance, null, null, null);
    }
}
