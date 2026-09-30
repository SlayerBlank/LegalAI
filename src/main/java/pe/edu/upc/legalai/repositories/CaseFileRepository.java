package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.CaseFile;

import java.util.List;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {

    List<CaseFile> findByClientClientId(Long clientId);

    List<CaseFile> findByOwnerUserUserId(Long userId);
}
