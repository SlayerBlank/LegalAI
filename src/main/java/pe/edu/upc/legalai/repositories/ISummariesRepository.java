package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.dtos.response.SummariesConsultaResponseDTO;
import pe.edu.upc.legalai.entities.Summaries;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ISummariesRepository
        extends JpaRepository<Summaries, Long> {

    List<Summaries> findByGeneratedByUserIdOrderBySummaryIdDesc(
            Long userId
    );

    Optional<Summaries> findBySummaryIdAndGeneratedByUserId(
            Long summaryId,
            Long userId
    );

    // HU-65: Resúmenes por expediente, tipo y fecha de creación
    @Query("""
            SELECT new pe.edu.upc.legalai.dtos.response.SummariesConsultaResponseDTO(
                s.summaryId,
                doc.documentId,
                s.summaryType,
                s.content,
                s.createdAt
            )
            FROM Summaries s
            LEFT JOIN s.expediente e
            LEFT JOIN s.documento doc
            LEFT JOIN doc.expediente de
            LEFT JOIN e.owner eo
            LEFT JOIN de.owner deo
            WHERE s.generatedBy.userId = :userId
              AND (e.caseId = :caseId OR de.caseId = :caseId)
              AND (e IS NULL OR eo.userId = :userId)
              AND (doc IS NULL OR deo.userId = :userId)
              AND (e IS NULL OR doc IS NULL OR e.caseId = de.caseId)
              AND s.summaryType = :summaryType
              AND s.createdAt >= :createdFrom
              AND s.createdAt < :createdToExclusive
            ORDER BY s.createdAt DESC, s.summaryId DESC
            """)
    List<SummariesConsultaResponseDTO> consultarPorExpedienteTipoYFecha(
            @Param("userId") Long userId,
            @Param("caseId") Long caseId,
            @Param("summaryType") String summaryType,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdToExclusive") LocalDateTime createdToExclusive
    );
}