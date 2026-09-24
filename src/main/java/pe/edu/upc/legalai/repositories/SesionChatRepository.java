package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.SesionChat;

import java.util.List;

@Repository
public interface SesionChatRepository extends JpaRepository<SesionChat, Long> {

    List<SesionChat> findByExpediente_CaseIdOrderByUpdatedAtDesc(Long caseId);

    List<SesionChat> findByUsuario_UserIdOrderByUpdatedAtDesc(Long userId);
}