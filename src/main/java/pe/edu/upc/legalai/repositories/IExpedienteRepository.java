package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.Expediente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IExpedienteRepository extends JpaRepository<Expediente, Long> {

    List<Expediente> findByOwnerUserId(Long userId);

    List<Expediente> findByClientClientIdAndOwnerUserId(Long clientId, Long userId);

    List<Expediente> findByClientClientIdAndOwnerUserIdAndStatusAndOpenedAtBetween(
            Long clientId, Long userId, EstadoExpediente status, LocalDate openedFrom, LocalDate openedTo);

    Optional<Expediente> findByCaseIdAndOwnerUserId(Long caseId, Long userId);
}
