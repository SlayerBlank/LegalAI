package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.dtos.response.DraftsConsultaResponseDTO;
import pe.edu.upc.legalai.entities.Drafts;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IDraftsRepository extends JpaRepository<Drafts, Long> {

    List<Drafts>
    findByCreatedByUserIdAndExpedienteOwnerUserIdOrderByDraftIdDesc(
            Long createdByUserId,
            Long ownerUserId
    );

    Optional<Drafts>
    findByDraftIdAndCreatedByUserIdAndExpedienteOwnerUserId(
            Long draftId,
            Long createdByUserId,
            Long ownerUserId
    );

    // HU-64: borradores por expediente, estado y fecha de modificación
    @Query("""
            SELECT new pe.edu.upc.legalai.dtos.response.DraftsConsultaResponseDTO(
                d.draftId,
                d.title,
                d.status,
                d.updatedAt
            )
            FROM Drafts d
            JOIN d.expediente e
            WHERE d.createdBy.userId = :userId
              AND e.owner.userId = :userId
              AND e.caseId = :caseId
              AND d.status = :status
              AND d.updatedAt >= :updatedFrom
              AND d.updatedAt < :updatedToExclusive
            ORDER BY d.updatedAt DESC, d.draftId DESC
            """)
    List<DraftsConsultaResponseDTO> consultarPorExpedienteEstadoYFecha(
            @Param("userId") Long userId,
            @Param("caseId") Long caseId,
            @Param("status") String status,
            @Param("updatedFrom") LocalDateTime updatedFrom,
            @Param("updatedToExclusive") LocalDateTime updatedToExclusive
    );
}