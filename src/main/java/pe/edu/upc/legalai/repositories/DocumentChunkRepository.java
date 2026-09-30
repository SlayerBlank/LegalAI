package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upc.legalai.entities.DocumentChunk;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentoDocumentIdOrderByChunkIndexAsc(Long documentId);
    long countByDocumentoDocumentId(Long documentId);

    @Modifying(flushAutomatically = true)
    @Query("delete from DocumentChunk c where c.documento.documentId = :documentId")
    void deleteByDocumentoDocumentId(@Param("documentId") Long documentId);
}
