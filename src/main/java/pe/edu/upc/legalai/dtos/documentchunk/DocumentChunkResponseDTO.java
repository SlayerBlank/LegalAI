package pe.edu.upc.legalai.dtos.documentchunk;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos de un fragmento de documento")
public class DocumentChunkResponseDTO {

    @Schema(description = "Identificador del fragmento", example = "1")
    private Long chunkId;

    @Schema(description = "Identificador del documento asociado", example = "1")
    private Long documentId;

    @Schema(description = "Posición del fragmento dentro del documento", example = "0")
    private Integer chunkIndex;

    @Schema(description = "Número de página del documento original", example = "1")
    private Integer pageNumber;

    @Schema(description = "Contenido de texto del fragmento")
    private String content;

    @Schema(description = "Referencia al vector de embedding generado", example = "emb-doc1-chunk0")
    private String embeddingRef;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    public DocumentChunkResponseDTO() {
    }

    public DocumentChunkResponseDTO(Long chunkId, Long documentId, Integer chunkIndex, Integer pageNumber,
                                    String content, String embeddingRef, LocalDateTime createdAt) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.content = content;
        this.embeddingRef = embeddingRef;
        this.createdAt = createdAt;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbeddingRef() {
        return embeddingRef;
    }

    public void setEmbeddingRef(String embeddingRef) {
        this.embeddingRef = embeddingRef;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
