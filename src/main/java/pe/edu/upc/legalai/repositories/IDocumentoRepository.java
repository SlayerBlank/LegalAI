package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Documento;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDocumentoRepository extends JpaRepository<Documento, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Documento d where d.documentId = :id and d.expediente.owner.userId = :userId")
    Optional<Documento> findOwnedForProcessing(@org.springframework.data.repository.query.Param("id") Long id,
                                             @org.springframework.data.repository.query.Param("userId") Long userId);

    List<Documento> findByExpedienteCaseIdAndExpedienteOwnerUserId(Long caseId, Long userId);

    Optional<Documento> findByDocumentIdAndExpedienteOwnerUserId(Long documentId, Long userId);

    Optional<Documento> findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(Long documentId, Long caseId,
                                                                                     Long userId);
}
