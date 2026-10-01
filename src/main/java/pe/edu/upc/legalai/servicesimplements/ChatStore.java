package pe.edu.upc.legalai.servicesimplements;

import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.response.ChatTurnMetadata;
import pe.edu.upc.legalai.dtos.response.RAGResponseDTO;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.entities.SesionChat;
import pe.edu.upc.legalai.entities.SenderType;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IMensajesRepository;
import pe.edu.upc.legalai.repositories.ISesionChatRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ChatStore {

    private static final Logger LOG = LoggerFactory.getLogger(ChatStore.class);

    public record Alcance(Long sessionId, Long caseId, Long documentId, boolean documentScopeRequired) {
        public Alcance(Long sessionId, Long caseId, Long documentId) {
            this(sessionId, caseId, documentId, documentId != null);
        }
    }

    private final ISesionChatRepository sesiones;
    private final IMensajesRepository mensajes;
    private final ObjectMapper mapper;

    public ChatStore(ISesionChatRepository sesiones, IMensajesRepository mensajes, ObjectMapper mapper) {
        this.sesiones = sesiones;
        this.mensajes = mensajes;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Alcance alcance(Long sessionId, Long userId) {
        SesionChat sesion = buscarSesion(sessionId);
        if (!userId.equals(sesion.getUsuario().getUserId())) {
            throw notFound(sessionId);
        }
        return new Alcance(sesion.getId(), sesion.getExpediente().getCaseId(),
                sesion.getDocumento() == null ? null : sesion.getDocumento().getDocumentId(),
                sesion.isDocumentScopeRequired());
    }

    @Transactional(readOnly = true)
    public List<Mensajes> historialReciente(Long sessionId, int limite, Long beforeMessageId) {
        List<Mensajes> recientes = mensajes
                .findBySesionIdAndMessageIdLessThanOrderByCreatedAtDescMessageIdDesc(
                        sessionId, beforeMessageId, PageRequest.of(0, limite));
        List<Mensajes> cronologico = new ArrayList<>(recientes);
        Collections.reverse(cronologico);
        return cronologico;
    }

    @Transactional(readOnly = true)
    public Optional<Mensajes> porClientMessageId(Long sessionId, String clientMessageId) {
        return clientMessageId == null ? Optional.empty()
                : mensajes.findBySesionIdAndClientMessageId(sessionId, clientMessageId);
    }

    @Transactional(readOnly = true)
    public Optional<Mensajes> respuestaDe(Long sessionId, Long messageId) {
        return mensajes.findReply(sessionId, messageId, SenderType.ASSISTANT);
    }

    @Transactional(readOnly = true)
    public ChatTurnMetadata metadata(Mensajes assistantMessage) {
        String value = assistantMessage.getResponseMetadata();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(value, ChatTurnMetadata.class);
        } catch (RuntimeException ex) {
            LOG.warn("Metadatos de respuesta de chat no disponibles type={}", ex.getClass().getSimpleName());
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mensajes guardarUsuario(Long sessionId, String content, String clientMessageId) {
        SesionChat sesion = buscarSesion(sessionId);
        Mensajes mensaje = new Mensajes();
        mensaje.setSesion(sesion);
        mensaje.setSenderType(SenderType.USER);
        mensaje.setContent(content);
        mensaje.setClientMessageId(clientMessageId);
        Mensajes guardado = mensajes.save(mensaje);
        tocar(sesion);
        return guardado;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mensajes guardarAsistente(Long sessionId, Long userMessageId, String content, RAGResponseDTO response) {
        SesionChat sesion = buscarSesion(sessionId);
        Mensajes userMessage = mensajes.findByMessageIdAndSesionIdAndSenderType(
                        userMessageId, sessionId, SenderType.USER)
                .orElseThrow(() -> notFound(sessionId));
        Mensajes mensaje = new Mensajes();
        mensaje.setSesion(sesion);
        mensaje.setRespuestaA(userMessage);
        mensaje.setSenderType(SenderType.ASSISTANT);
        mensaje.setContent(content);
        mensaje.setResponseMetadata(mapper.writeValueAsString(new ChatTurnMetadata(
                response.provider(), response.model(), response.retrievedChunks(),
                response.sourcesType() == null ? "CONSULTED_FRAGMENTS" : response.sourcesType(),
                response.sources())));
        Mensajes guardado = mensajes.save(mensaje);
        tocar(sesion);
        return guardado;
    }

    private SesionChat buscarSesion(Long sessionId) {
        return sesiones.findById(sessionId).orElseThrow(() -> notFound(sessionId));
    }

    private void tocar(SesionChat sesion) {
        sesion.setUpdatedAt(LocalDateTime.now());
        sesiones.save(sesion);
    }

    private static ResourceNotFoundException notFound(Long sessionId) {
        return new ResourceNotFoundException("Sesion de chat no encontrada con id: " + sessionId);
    }
}