package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Drafts;

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
}