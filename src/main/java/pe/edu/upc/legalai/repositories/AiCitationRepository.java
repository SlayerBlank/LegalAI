package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.AiCitation;

import java.util.List;

@Repository
public interface AiCitationRepository extends JpaRepository<AiCitation, Long> {

    List<AiCitation> findByMessageMessageId(Long messageId);
}
