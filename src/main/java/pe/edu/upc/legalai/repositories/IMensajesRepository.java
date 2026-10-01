package pe.edu.upc.legalai.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.entities.SenderType;

import java.util.List;
import java.util.Optional;

@Repository
public interface IMensajesRepository extends JpaRepository<Mensajes, Long> {

    List<Mensajes> findBySesionIdOrderByCreatedAtAscMessageIdAsc(Long sessionId, Pageable pageable);

    List<Mensajes> findBySesionIdAndMessageIdLessThanOrderByCreatedAtDescMessageIdDesc(
            Long sessionId, Long messageId, Pageable pageable);

    Optional<Mensajes> findBySesionIdAndClientMessageId(Long sessionId, String clientMessageId);

    Optional<Mensajes> findByMessageIdAndSesionIdAndSenderType(Long messageId, Long sessionId, SenderType senderType);

    @Query("select m from Mensajes m where m.sesion.id = :sessionId "
            + "and m.respuestaA.messageId = :userMessageId and m.senderType = :senderType")
    Optional<Mensajes> findReply(@Param("sessionId") Long sessionId,
                                 @Param("userMessageId") Long userMessageId,
                                 @Param("senderType") SenderType senderType);

    long countBySesionId(Long sessionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Mensajes m where m.sesion.id = :sessionId")
    void deleteBySesionId(@Param("sessionId") Long sessionId);
}