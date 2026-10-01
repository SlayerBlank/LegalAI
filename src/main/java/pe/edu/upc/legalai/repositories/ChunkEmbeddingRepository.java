package pe.edu.upc.legalai.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.dtos.response.SemanticSearchResultDTO;
import pe.edu.upc.legalai.configs.EmbeddingSettings;
import pe.edu.upc.legalai.exceptions.EmbeddingException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Native vector access. JdbcTemplate participates in the existing JPA transaction.
 * This specialized JDBC/pgvector class intentionally remains outside the I*Repository
 * naming convention used for JPA interfaces.
 */
@Repository
public class ChunkEmbeddingRepository {
    private final JdbcTemplate jdbc;
    private final EmbeddingSettings settings;

    public ChunkEmbeddingRepository(JdbcTemplate jdbc, EmbeddingSettings settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    public void validateSchema() {
        var types = jdbc.queryForList("""
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                WHERE a.attrelid = to_regclass('document_chunks')
                  AND a.attname = 'embedding' AND NOT a.attisdropped
                """, String.class);
        if (types.size() != 1 || !types.getFirst().equals("vector(" + settings.dimension() + ")"))
            throw new EmbeddingException("Ejecute la migracion pgvector; la columna embedding debe ser vector("
                    + settings.dimension() + ")");
    }

    public Set<Long> validChunkIds(Long documentId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT chunk_id FROM document_chunks
                WHERE document_id = ? AND embedding IS NOT NULL AND embedding_model = ?
                  AND vector_dims(embedding) = ? AND vector_norm(embedding) > 0
                """, Long.class, documentId, settings.model(), settings.dimension()));
    }

    public long countValidChunks(Long documentId) {
        return java.util.Objects.requireNonNull(jdbc.queryForObject("""
                SELECT count(*) FROM document_chunks
                WHERE document_id = ? AND embedding IS NOT NULL AND embedding_model = ?
                  AND vector_dims(embedding) = ? AND vector_norm(embedding) > 0
                """, Long.class, documentId, settings.model(), settings.dimension()));
    }

    public void save(Long documentId, Long chunkId, float[] vector) {
        int updated = jdbc.update("""
                UPDATE document_chunks SET embedding = CAST(? AS vector), embedding_model = ?
                WHERE document_id = ? AND chunk_id = ?
                """, settings.vectorLiteral(vector), settings.model(), documentId, chunkId);
        if (updated != 1) throw new EmbeddingException("El chunk " + chunkId + " ya no existe");
    }

    public List<SemanticSearchResultDTO> searchDocument(Long documentId, Long userId, float[] query, int topK) {
        return search(false, documentId, userId, query, topK);
    }

    public List<SemanticSearchResultDTO> searchCase(Long caseId, Long userId, float[] query, int topK) {
        return search(true, caseId, userId, query, topK);
    }

    private List<SemanticSearchResultDTO> search(boolean byCase, Long id, Long userId, float[] query, int topK) {
        // Only the fixed scope column is assembled; all external values remain bound parameters.
        String scope = byCase ? "d.case_id" : "d.document_id";
        String sql = """
                SELECT c.chunk_id, c.document_id, c.chunk_index, c.content, d.file_name, c.char_start, c.char_end,
                       c.embedding <=> CAST(? AS vector) AS distance
                FROM document_chunks c
                JOIN documents d ON d.document_id = c.document_id
                JOIN cases e ON e.case_id = d.case_id
                WHERE %s = ? AND e.owner_user_id = ?
                  AND c.embedding IS NOT NULL AND c.embedding_model = ?
                  AND vector_dims(c.embedding) = ? AND vector_norm(c.embedding) > 0
                ORDER BY distance ASC, c.chunk_id ASC LIMIT ?
                """.formatted(scope);
        return jdbc.query(sql, (rs, row) -> new SemanticSearchResultDTO(rs.getLong("chunk_id"),
                rs.getLong("document_id"), rs.getInt("chunk_index"), rs.getString("content"),
                rs.getDouble("distance"), rs.getString("file_name"), rs.getInt("char_start"), rs.getInt("char_end")), settings.vectorLiteral(query), id, userId,
                settings.model(), settings.dimension(), topK);
    }
}
