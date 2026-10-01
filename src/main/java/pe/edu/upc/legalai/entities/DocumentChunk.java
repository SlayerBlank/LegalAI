package pe.edu.upc.legalai.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks", uniqueConstraints =
        @UniqueConstraint(name = "uk_document_chunks_document_index", columnNames = {"document_id", "chunk_index"}))
public class DocumentChunk {
    // embedding vector(N) and embedding_model are managed by ChunkEmbeddingRepository.
    // Deliberately excluded from ORM mapping: Hibernate ddl-auto cannot install pgvector.
    // See database/migrations/20261002_chunk_embeddings.sql (default N = 768).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Documento documento;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "char_start", nullable = false)
    private int charStart;
    @Column(name = "char_end", nullable = false)
    private int charEnd;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getChunkId() { return chunkId; }
    public Documento getDocumento() { return documento; }
    public void setDocumento(Documento documento) { this.documento = documento; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getCharStart() { return charStart; }
    public void setCharStart(int charStart) { this.charStart = charStart; }
    public int getCharEnd() { return charEnd; }
    public void setCharEnd(int charEnd) { this.charEnd = charEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
