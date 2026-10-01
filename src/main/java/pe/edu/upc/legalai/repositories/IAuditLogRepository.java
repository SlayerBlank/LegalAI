package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.AuditLog;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLog, Long> {
}
