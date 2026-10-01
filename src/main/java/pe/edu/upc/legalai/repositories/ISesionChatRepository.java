package pe.edu.upc.legalai.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.SesionChat;

import java.util.List;

@Repository
public interface ISesionChatRepository extends JpaRepository<SesionChat, Long> {

    List<SesionChat> findByExpediente_CaseIdOrderByUpdatedAtDesc(Long caseId);

    List<SesionChat> findByUsuario_UserIdOrderByUpdatedAtDesc(Long userId);

    List<SesionChat> findByUsuario_UserIdOrderByUpdatedAtDescIdDesc(Long userId, Pageable pageable);

    List<SesionChat> findByUsuario_UserIdAndExpediente_CaseIdOrderByUpdatedAtDescIdDesc(Long userId, Long caseId,
                                                                                               Pageable pageable);
}