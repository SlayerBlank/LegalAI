package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.Expediente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    List<Expediente> findByOwnerUserId(Long userId);

    List<Expediente> findByClientClientIdAndOwnerUserId(Long clientId, Long userId);

    Optional<Expediente> findByCaseIdAndOwnerUserId(Long caseId, Long userId);

    @Query("""
            select e.caseId as caseId,
                   e.title as title,
                   c.fullNameOrCompany as clientName,
                   e.status as status,
                   e.openedAt as openedAt,
                   u.userId as lawyerId,
                   u.fullName as lawyerName,
                   u.email as lawyerEmail,
                   u.status as lawyerStatus,
                   u.createdAt as lawyerCreatedAt,
                   u.updatedAt as lawyerUpdatedAt,
                   r.roleId as lawyerRoleId,
                   r.name as lawyerRoleName
            from Expediente e
            join e.client c
            join e.owner u
            left join u.rol r
            where u.userId = :usuarioId
              and e.status = :status
            order by e.openedAt desc, e.caseId desc
            """)
    List<ExpedienteAbiertoConAbogadoProjection> listarAbiertosConAbogadoPorUsuario(Long usuarioId, EstadoExpediente status);

    interface ExpedienteAbiertoConAbogadoProjection {
        Long getCaseId();
        String getTitle();
        String getClientName();
        EstadoExpediente getStatus();
        LocalDate getOpenedAt();
        Long getLawyerId();
        String getLawyerName();
        String getLawyerEmail();
        String getLawyerStatus();
        LocalDateTime getLawyerCreatedAt();
        LocalDateTime getLawyerUpdatedAt();
        Long getLawyerRoleId();
        String getLawyerRoleName();
    }
}
