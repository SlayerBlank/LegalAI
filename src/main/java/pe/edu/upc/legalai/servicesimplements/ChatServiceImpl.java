package pe.edu.upc.legalai.servicesimplements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.request.ChatCreateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.ChatHistoryTurnDTO;
import pe.edu.upc.legalai.dtos.request.ChatSendMessageRequestDTO;
import pe.edu.upc.legalai.dtos.request.ChatUpdateSessionRequestDTO;
import pe.edu.upc.legalai.dtos.request.RAGRequestDTO;
import pe.edu.upc.legalai.dtos.response.ChatMessageDTO;
import pe.edu.upc.legalai.dtos.response.ChatMessageResponseDTO;
import pe.edu.upc.legalai.dtos.response.ChatSessionResponseDTO;
import pe.edu.upc.legalai.dtos.response.ChatTurnMetadata;
import pe.edu.upc.legalai.dtos.response.RAGResponseDTO;
import pe.edu.upc.legalai.configs.ChatSettings;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.entities.SesionChat;
import pe.edu.upc.legalai.entities.SenderType;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.repositories.IMensajesRepository;
import pe.edu.upc.legalai.repositories.ISesionChatRepository;
import pe.edu.upc.legalai.servicesinterfaces.ChatService;
import pe.edu.upc.legalai.servicesinterfaces.RAGService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final String TITULO_POR_DEFECTO = "Nueva conversacion";
    private static final String SOURCES_TYPE = "CONSULTED_FRAGMENTS";

    private final ISesionChatRepository sesiones;
    private final IMensajesRepository mensajes;
    private final IExpedienteRepository expedientes;
    private final IDocumentoRepository documentos;
    private final IUsuarioService usuarios;
    private final RAGService rag;
    private final ChatStore store;
    private final ChatAuditService auditoria;
    private final ChatSettings settings;

    public ChatServiceImpl(ISesionChatRepository sesiones, IMensajesRepository mensajes,
            IExpedienteRepository expedientes, IDocumentoRepository documentos, IUsuarioService usuarios,
            RAGService rag, ChatStore store, ChatAuditService auditoria, ChatSettings settings) {
        this.sesiones = sesiones;
        this.mensajes = mensajes;
        this.expedientes = expedientes;
        this.documentos = documentos;
        this.usuarios = usuarios;
        this.rag = rag;
        this.store = store;
        this.auditoria = auditoria;
        this.settings = settings;
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO crear(ChatCreateSessionRequestDTO request) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        Long caseId = request.getCaseId();
        if (caseId == null || caseId <= 0) {
            throw new BadRequestException("caseId es obligatorio y debe ser un identificador valido");
        }
        Expediente expediente = buscarExpedientePropio(caseId, usuario.getUserId());
        Documento documento = validarDocumento(request.getDocumentId(), caseId, usuario.getUserId());

        SesionChat sesion = new SesionChat();
        sesion.setExpediente(expediente);
        sesion.setUsuario(usuario);
        sesion.setDocumento(documento);
        sesion.setDocumentScopeRequired(documento != null);
        sesion.setTitulo(tituloOporDefecto(request.getTitle()));
        SesionChat guardada = sesiones.save(sesion);
        auditoria.registrar(usuario, "CREATE_CHAT_SESSION", guardada.getId(),
                "caseId=" + caseId + "; scope=" + (documento == null ? "Expediente" : "Documento"));
        return toResponse(guardada, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO> listar(Long caseId, Integer page, Integer size) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        if (caseId != null) {
            buscarExpedientePropio(caseId, usuario.getUserId());
        }
        PageRequest pageable = PageRequest.of(normalizarPagina(page), normalizarTamano(size));
        List<SesionChat> propias = caseId == null
                ? sesiones.findByUsuario_UserIdOrderByUpdatedAtDescIdDesc(usuario.getUserId(), pageable)
                : sesiones.findByUsuario_UserIdAndExpediente_CaseIdOrderByUpdatedAtDescIdDesc(
                        usuario.getUserId(), caseId, pageable);
        return propias.stream().map(sesion -> toResponse(sesion, mensajes.countBySesionId(sesion.getId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO> filtrarPorExpedienteYRangoActividad(Long caseId, LocalDateTime from, LocalDateTime to) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        if (caseId == null || caseId <= 0) {
            throw new BadRequestException("caseId es obligatorio y debe ser un identificador valido");
        }
        if (from == null || to == null) {
            throw new BadRequestException("from y to son obligatorios");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from no puede ser posterior a to");
        }
        buscarExpedientePropio(caseId, usuario.getUserId());
        List<SesionChat> sesionesFiltradas = sesiones.findByUsuario_UserIdAndExpediente_CaseIdAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                usuario.getUserId(), caseId, from, to);
        return sesionesFiltradas.stream()
                .map(sesion -> toResponse(sesion, mensajes.countBySesionId(sesion.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionResponseDTO obtener(Long sessionId) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        SesionChat sesion = buscarSesionPropia(sessionId, usuario);
        return toResponse(sesion, mensajes.countBySesionId(sesion.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> historial(Long sessionId, Integer page, Integer size) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        buscarSesionPropia(sessionId, usuario);
        return mensajes.findBySesionIdOrderByCreatedAtAscMessageIdAsc(sessionId,
                        PageRequest.of(normalizarPagina(page), normalizarTamano(size))).stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO actualizarTitulo(Long sessionId, ChatUpdateSessionRequestDTO request) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        String titulo = request.getTitle() == null ? null : request.getTitle().trim();
        if (titulo == null || titulo.isEmpty()) {
            throw new BadRequestException("El titulo es obligatorio");
        }
        SesionChat sesion = buscarSesionPropia(sessionId, usuario);
        sesion.setTitulo(titulo);
        SesionChat guardada = sesiones.save(sesion);
        auditoria.registrar(usuario, "UPDATE_CHAT_SESSION", sessionId, "title=UPDATED");
        return toResponse(guardada, mensajes.countBySesionId(sessionId));
    }

    @Override
    @Transactional
    public void eliminar(Long sessionId) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        SesionChat sesion = buscarSesionPropia(sessionId, usuario);
        Long caseId = sesion.getExpediente().getCaseId();
        mensajes.deleteBySesionId(sessionId);
        sesiones.delete(sesion);
        auditoria.registrar(usuario, "DELETE_CHAT_SESSION", sessionId, "caseId=" + caseId + "; messages=DELETED");
    }

    @Override
    public ChatMessageResponseDTO enviarMensaje(Long sessionId, ChatSendMessageRequestDTO request) {
        Usuario usuario = usuarios.obtenerUsuarioAutenticado();
        ChatStore.Alcance alcance = store.alcance(sessionId, usuario.getUserId());
        validarAlcance(alcance, usuario.getUserId());
        if (alcance.documentScopeRequired() && alcance.documentId() == null) {
            throw new ResourceNotFoundException("El documento de alcance de la sesion ya no esta disponible");
        }
        String contenido = validarContenido(request.getContent());
        String clientMessageId = normalizarClientMessageId(request.getClientMessageId());

        Optional<Mensajes> previo = store.porClientMessageId(sessionId, clientMessageId);
        if (previo.isPresent()) {
            if (!previo.get().getContent().equals(contenido)) {
                throw new BadRequestException("clientMessageId ya fue usado con otro contenido");
            }
            Optional<Mensajes> assistant = store.respuestaDe(sessionId, previo.get().getMessageId());
            if (assistant.isPresent()) {
                LOG.info("Chat idempotente sessionId={} messageId={}", sessionId, previo.get().getMessageId());
                return respuestaGuardada(sessionId, previo.get(), assistant.get());
            }
            LOG.info("Reintento de turno todavia sin respuesta sessionId={} messageId={}", sessionId,
                    previo.get().getMessageId());
        }

        Mensajes userMessage = previo.orElse(null);
        if (userMessage == null) {
            try {
                userMessage = store.guardarUsuario(sessionId, contenido, clientMessageId);
            } catch (DataIntegrityViolationException duplicateRequest) {
                if (clientMessageId == null) {
                    throw duplicateRequest;
                }
                userMessage = store.porClientMessageId(sessionId, clientMessageId).orElseThrow(() -> duplicateRequest);
                if (!userMessage.getContent().equals(contenido)) {
                    throw new BadRequestException("clientMessageId ya fue usado con otro contenido");
                }
                Optional<Mensajes> assistant = store.respuestaDe(sessionId, userMessage.getMessageId());
                if (assistant.isPresent()) {
                    return respuestaGuardada(sessionId, userMessage, assistant.get());
                }
            }
        }

        List<ChatHistoryTurnDTO> historial = store.historialReciente(
                        sessionId, settings.maxHistoryMessages(), userMessage.getMessageId()).stream()
                .map(ChatServiceImpl::toTurno)
                .toList();

        String scope = alcance.documentId() == null ? "Expediente" : "Documento";
        RAGRequestDTO ragRequest = new RAGRequestDTO();
        ragRequest.setQuestion(contenido);
        RAGResponseDTO respuesta;
        try {
            respuesta = alcance.documentId() == null
                    ? rag.preguntarExpedienteConversacional(alcance.caseId(), ragRequest, historial)
                    : rag.preguntarDocumentoConversacional(alcance.documentId(), ragRequest, historial);
        } catch (RuntimeException ex) {
            // The USER turn stays persisted without an invented ASSISTANT answer; only metadata is audited.
            LOG.warn("Chat sin respuesta del modelo sessionId={} scope={} type={}", sessionId, scope,
                    ex.getClass().getSimpleName());
            registrarFallo(usuario, sessionId, alcance.caseId(), scope, ex);
            throw ex;
        }
        if (respuesta == null || respuesta.answer() == null || respuesta.answer().isBlank()) {
            registrarFallo(usuario, sessionId, alcance.caseId(), scope, new IAServiceException());
            throw new IAServiceException();
        }

        Mensajes assistantMessage;
        try {
            assistantMessage = store.guardarAsistente(
                    sessionId, userMessage.getMessageId(), respuesta.answer(), respuesta);
        } catch (DataIntegrityViolationException duplicateResponse) {
            Optional<Mensajes> savedResponse = store.respuestaDe(sessionId, userMessage.getMessageId());
            if (savedResponse.isEmpty()) {
                throw duplicateResponse;
            }
            return respuestaGuardada(sessionId, userMessage, savedResponse.get());
        }
        auditoria.registrar(usuario, "SEND_CHAT_MESSAGE", sessionId, "caseId=" + alcance.caseId()
                + "; scope=" + scope + "; retrievedChunks=" + respuesta.retrievedChunks() + "; result=SUCCESS");
        return new ChatMessageResponseDTO(sessionId, toMessage(userMessage), toMessage(assistantMessage),
                respuesta.provider(), respuesta.model(), respuesta.retrievedChunks(),
                respuesta.sourcesType() == null ? SOURCES_TYPE : respuesta.sourcesType(), respuesta.sources());
    }

    private ChatMessageResponseDTO respuestaGuardada(Long sessionId, Mensajes userMessage, Mensajes assistantMessage) {
        ChatTurnMetadata metadata = store.metadata(assistantMessage);
        return new ChatMessageResponseDTO(sessionId, toMessage(userMessage), toMessage(assistantMessage),
                metadata == null ? null : metadata.provider(), metadata == null ? null : metadata.model(),
                metadata == null ? null : metadata.retrievedChunks(),
                metadata == null ? null : metadata.sourcesType(),
                metadata == null ? null : metadata.sources());
    }

    private void registrarFallo(Usuario usuario, Long sessionId, Long caseId, String scope, RuntimeException error) {
        try {
            auditoria.registrarFallo(usuario, "SEND_CHAT_MESSAGE", sessionId,
                    "caseId=" + caseId + "; scope=" + scope + "; result=FAILED; type=" + error.getClass().getSimpleName());
        } catch (RuntimeException auditError) {
            LOG.error("Fallo al auditar el chat sessionId={} type={}", sessionId,
                    auditError.getClass().getSimpleName());
        }
    }

    private void validarAlcance(ChatStore.Alcance alcance, Long userId) {
        buscarExpedientePropio(alcance.caseId(), userId);
        if (alcance.documentId() != null) {
            validarDocumento(alcance.documentId(), alcance.caseId(), userId);
        }
    }

    private Documento validarDocumento(Long documentId, Long caseId, Long userId) {
        if (documentId == null) {
            return null;
        }
        if (documentId <= 0) {
            throw new BadRequestException("documentId debe ser un identificador valido");
        }
        return documentos.findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(documentId, caseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + documentId));
    }

    private String validarContenido(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("El contenido del mensaje es obligatorio");
        }
        String limpio = content.trim();
        if (limpio.length() > settings.maxMessageChars()) {
            throw new BadRequestException("El mensaje no puede superar " + settings.maxMessageChars() + " caracteres");
        }
        return limpio;
    }

    private String normalizarClientMessageId(String clientMessageId) {
        if (clientMessageId == null) {
            return null;
        }
        String limpio = clientMessageId.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (!limpio.matches("[A-Za-z0-9._:-]{1,64}")) {
            throw new BadRequestException("clientMessageId admite hasta 64 caracteres de [A-Za-z0-9._:-]");
        }
        return limpio;
    }

    private Expediente buscarExpedientePropio(Long caseId, Long userId) {
        return expedientes.findByCaseIdAndOwnerUserId(caseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con id: " + caseId));
    }

    private SesionChat buscarSesionPropia(Long sessionId, Usuario usuario) {
        SesionChat sesion = sesiones.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesion de chat no encontrada con id: " + sessionId));
        if (!usuario.getUserId().equals(sesion.getUsuario().getUserId())) {
            throw new ResourceNotFoundException("Sesion de chat no encontrada con id: " + sessionId);
        }
        return sesion;
    }

    private int normalizarPagina(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizarTamano(Integer size) {
        if (size == null || size <= 0) {
            return settings.defaultPageSize();
        }
        return Math.min(size, settings.maxPageSize());
    }

    private String tituloOporDefecto(String titulo) {
        return titulo == null || titulo.isBlank() ? TITULO_POR_DEFECTO : titulo.trim();
    }

    private ChatSessionResponseDTO toResponse(SesionChat sesion, long messageCount) {
        return new ChatSessionResponseDTO(sesion.getId(), sesion.getExpediente().getCaseId(),
                sesion.getDocumento() == null ? null : sesion.getDocumento().getDocumentId(),
                sesion.isDocumentScopeRequired(), sesion.getTitulo(),
                messageCount, sesion.getCreatedAt(), sesion.getUpdatedAt());
    }

    private ChatMessageDTO toMessage(Mensajes mensaje) {
        ChatTurnMetadata metadata = mensaje.getSenderType() == SenderType.ASSISTANT
                ? store.metadata(mensaje) : null;
        return new ChatMessageDTO(mensaje.getMessageId(), mensaje.getSenderType().name(), mensaje.getContent(),
                mensaje.getCreatedAt(), metadata == null ? null : metadata.provider(),
                metadata == null ? null : metadata.model(), metadata == null ? null : metadata.retrievedChunks(),
                metadata == null ? null : metadata.sourcesType(), metadata == null ? null : metadata.sources());
    }

    private static ChatHistoryTurnDTO toTurno(Mensajes mensaje) {
        return new ChatHistoryTurnDTO(mensaje.getSenderType() == SenderType.USER ? "USER" : "ASSISTANT",
                mensaje.getContent());
    }
}