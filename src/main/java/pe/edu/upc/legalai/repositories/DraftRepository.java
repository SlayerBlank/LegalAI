package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Draft;

import java.util.List;

@Repository
public interface DraftRepository extends JpaRepository<Draft, Long> {

    List<Draft> findByCaseFileCaseId(Long caseId);
}
