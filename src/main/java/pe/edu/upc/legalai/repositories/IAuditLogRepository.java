package pe.edu.upc.legalai.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import pe.edu.upc.legalai.entities.AuditLog;

import java.util.List;

@Repository
public interface IAuditLogRepository
        extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    // Carga el usuario en la misma consulta: el DTO expone su nombre y correo (evita N+1).
    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    List<AuditLog> findByUsuarioUserIdOrderByCreatedAtDescLogIdDesc(Long userId);

    boolean existsByActionAndEntityTypeAndEntityIdAndUsuarioUserIdAndDetails(
            String action,
            String entityType,
            Long entityId,
            Long userId,
            String details
    );
}