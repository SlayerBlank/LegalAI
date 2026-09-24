package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Mensajes;

import java.util.List;

@Repository
public interface MensajesRepository extends JpaRepository<Mensajes, Long> {

    List<Mensajes> findAllByOrderByCreatedAtAsc();
}
