package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.CitacionesIA;

import java.util.List;

@Repository
public interface CitacionesIARepository extends JpaRepository<CitacionesIA, Long> {

    List<CitacionesIA> findByMensajeMessageIdOrderByCitationIdAsc(Long messageId);

    void deleteByMensajeMessageId(Long messageId);
}
