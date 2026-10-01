package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upc.legalai.entities.CitacionesIA;
import java.util.List;

public interface ICitacionesIARepository extends JpaRepository<CitacionesIA, Long> {
    List<CitacionesIA> findByMensajeMessageIdOrderByCitationIdAsc(Long messageId);
}
