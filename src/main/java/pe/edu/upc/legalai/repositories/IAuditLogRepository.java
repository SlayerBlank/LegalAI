package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import pe.edu.upc.legalai.entities.AuditLog;

import java.util.List;

@Repository
public interface IAuditLogRepository
        extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByUsuarioUserIdOrderByCreatedAtDescLogIdDesc(Long userId);

    boolean existsByActionAndEntityTypeAndEntityIdAndUsuarioUserIdAndDetails(
            String action,
            String entityType,
            Long entityId,
            Long userId,
            String details
    );
}