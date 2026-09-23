package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Documento;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByExpedienteCaseIdAndExpedienteOwnerUserId(Long caseId, Long userId);

    Optional<Documento> findByDocumentIdAndExpedienteOwnerUserId(Long documentId, Long userId);
}
