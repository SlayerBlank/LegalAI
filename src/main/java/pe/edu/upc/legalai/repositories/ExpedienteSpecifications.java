package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.Expediente;

import java.time.LocalDate;

public final class ExpedienteSpecifications {

    private ExpedienteSpecifications() {
    }

    public static Specification<Expediente> conFiltros(Long ownerUserId, EstadoExpediente status,
                                                        LocalDate openedFrom, LocalDate openedTo) {
        return (root, query, cb) -> {
            var predicates = cb.equal(root.get("owner").get("userId"), ownerUserId);
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (openedFrom != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("openedAt"), openedFrom));
            }
            if (openedTo != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("openedAt"), openedTo));
            }
            return predicates;
        };
    }
}
