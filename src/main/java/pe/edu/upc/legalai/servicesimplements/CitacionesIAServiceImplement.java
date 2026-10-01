package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.dtos.response.RAGSourceDTO;
import pe.edu.upc.legalai.entities.CitacionesIA;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.entities.SenderType;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.*;
import pe.edu.upc.legalai.servicesinterfaces.ICitacionesIAService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
public class CitacionesIAServiceImplement implements ICitacionesIAService {
    private final ICitacionesIARepository citations;
    private final IMensajesRepository messages;
    private final IDocumentoRepository documents;
    private final IDocumentChunkRepository chunks;
    private final IUsuarioService users;

    public CitacionesIAServiceImplement(ICitacionesIARepository citations, IMensajesRepository messages,
            IDocumentoRepository documents, IDocumentChunkRepository chunks, IUsuarioService users) {
        this.citations = citations;
        this.messages = messages;
        this.documents = documents;
        this.chunks = chunks;
        this.users = users;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Mensajes respuesta, List<RAGSourceDTO> sources) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        verifyOwner(respuesta, userId);
        if (sources == null || sources.stream().anyMatch(s -> s == null || s.documentId() == null
                || s.chunkId() == null || !Double.isFinite(s.distance()))) {
            throw new BadRequestException("Las fuentes de la respuesta no son validas");
        }
        // Stable lock ordering avoids deadlocks when two answers cite several documents.
        var ordered = sources.stream().sorted(Comparator.comparing(RAGSourceDTO::documentId)
                .thenComparing(RAGSourceDTO::chunkId)).toList();
        var seen = new HashSet<Long>();
        var persisted = new ArrayList<CitacionesIA>();
        for (RAGSourceDTO source : ordered) {
            if (!seen.add(source.chunkId())) continue;
            var document = documents.findOwnedForProcessing(source.documentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento de la fuente no disponible"));
            var session = respuesta.getSesion();
            if (!session.getExpediente().getCaseId().equals(document.getExpediente().getCaseId())
                    || (session.isDocumentScopeRequired() && (session.getDocumento() == null
                    || !session.getDocumento().getDocumentId().equals(source.documentId())))) {
                throw new ResourceNotFoundException("Fuente fuera del alcance de la conversacion");
            }
            var chunk = chunks.findById(source.chunkId())
                    .filter(c -> c.getDocumento().getDocumentId().equals(source.documentId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Fragmento de la fuente no disponible"));
            CitacionesIA citation = new CitacionesIA();
            citation.setMensaje(respuesta);
            citation.setDocumento(document);
            citation.setChunkId(chunk.getChunkId());
            citation.setRelevanceScore(1.0 - source.distance());
            persisted.add(citation);
        }
        citations.saveAll(persisted);
        respuesta.getCitaciones().addAll(persisted);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitacionIAResponseDTO> listarPorMensaje(Long sessionId, Long messageId) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        var message = messages.findByMessageIdAndSesionIdAndSenderType(messageId, sessionId, SenderType.ASSISTANT)
                .orElseThrow(() -> new ResourceNotFoundException("Respuesta de chat no encontrada"));
        verifyOwner(message, userId);
        return citations.findByMensajeMessageIdOrderByCitationIdAsc(messageId).stream()
                .map(c -> new CitacionIAResponseDTO(c.getCitationId(), messageId,
                        c.getDocumento() == null ? null : c.getDocumento().getDocumentId(),
                        c.getChunkId(), c.getRelevanceScore())).toList();
    }

    private void verifyOwner(Mensajes message, Long userId) {
        if (message.getSenderType() != SenderType.ASSISTANT
                || !userId.equals(message.getSesion().getUsuario().getUserId())
                || !userId.equals(message.getSesion().getExpediente().getOwner().getUserId())) {
            throw new ResourceNotFoundException("Respuesta de chat no encontrada");
        }
    }
}
