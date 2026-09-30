package pe.edu.upc.legalai.dtos.documentchunk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para crear un fragmento de documento")
public class DocumentChunkRequestDTO {

    @Schema(description = "Identificador del documento asociado", example = "1")
    @NotNull(message = "El documento es obligatorio")
    private Long documentId;

    @Schema(description = "Posición del fragmento dentro del documento", example = "0")
    @NotNull(message = "El índice del fragmento es obligatorio")
    @PositiveOrZero(message = "El índice del fragmento no puede ser negativo")
    private Integer chunkIndex;

    @Schema(description = "Número de página del documento original", example = "1")
    @Positive(message = "El número de página debe ser mayor a cero")
    private Integer pageNumber;

    @Schema(description = "Contenido de texto del fragmento", example = "Cláusula tercera: ...")
    @NotBlank(message = "El contenido del fragmento es obligatorio")
    private String content;

    @Schema(description = "Referencia al vector de embedding generado", example = "emb-doc1-chunk0")
    @Size(max = 255, message = "La referencia de embedding no debe superar los 255 caracteres")
    private String embeddingRef;

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
}
