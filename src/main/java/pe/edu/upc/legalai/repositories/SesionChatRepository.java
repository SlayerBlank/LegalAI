package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.SesionChat;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SesionChatRepository extends JpaRepository<SesionChat, Long> {

    List<SesionChat> findByUsuario_UserIdAndExpediente_CaseIdOrderByUpdatedAtDesc(Long userId, Long caseId);

    List<SesionChat> findByUsuario_UserIdAndExpediente_CaseIdAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            Long userId, Long caseId, LocalDateTime from, LocalDateTime to);

    List<SesionChat> findByUsuario_UserIdOrderByUpdatedAtDesc(Long userId);
}