package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.legalai.entities.AuditLog;

import java.time.LocalDateTime;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> conFiltros(Long userId, String action, String entityType,
                                                      LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (userId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("usuario").get("userId"), userId));
            }
            if (action != null && !action.isBlank()) {
                predicates = cb.and(predicates, cb.equal(cb.lower(root.get("action")), action.toLowerCase()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates = cb.and(predicates, cb.equal(cb.lower(root.get("entityType")), entityType.toLowerCase()));
            }
            if (from != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicates;
        };
    }
}
