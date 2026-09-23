package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Expediente;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    List<Expediente> findByOwnerUserId(Long userId);

    List<Expediente> findByClientClientIdAndOwnerUserId(Long clientId, Long userId);

    Optional<Expediente> findByCaseIdAndOwnerUserId(Long caseId, Long userId);
}
