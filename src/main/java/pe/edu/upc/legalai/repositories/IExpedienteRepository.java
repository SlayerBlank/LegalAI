package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.dtos.response.ExpedienteCantidadSesionesResponseDTO;
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

    List<Expediente> findByOwnerUserIdAndStatusOrderByUpdatedAtDesc(Long userId, EstadoExpediente status);

    Optional<Expediente> findByCaseIdAndOwnerUserId(Long caseId, Long userId);
    // HU-61: titulo del expediente y cantidad de sesiones de chat asociadas
    @Query("""
            SELECT new pe.edu.upc.legalai.dtos.response.ExpedienteCantidadSesionesResponseDTO(
                       e.caseId, e.title, COUNT(s))
            FROM Expediente e
            LEFT JOIN SesionChat s ON s.expediente = e
            WHERE e.owner.userId = :userId
            GROUP BY e.caseId, e.title
            ORDER BY COUNT(s) DESC
            """)
    List<ExpedienteCantidadSesionesResponseDTO> contarSesionesPorExpediente(@Param("userId") Long userId);
}
