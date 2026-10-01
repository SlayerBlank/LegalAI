package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import pe.edu.upc.legalai.DTOs.request.ChatCreateSessionRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ChatSendMessageRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ChatUpdateSessionRequestDTO;
import pe.edu.upc.legalai.DTOs.request.RAGRequestDTO;
import pe.edu.upc.legalai.DTOs.response.RAGResponseDTO;
import pe.edu.upc.legalai.DTOs.response.RAGSourceDTO;
import pe.edu.upc.legalai.DTOs.response.ChatTurnMetadata;
import pe.edu.upc.legalai.config.ChatSettings;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.entities.SesionChat;
import pe.edu.upc.legalai.entities.SenderType;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.DocumentoRepository;
import pe.edu.upc.legalai.repositories.ExpedienteRepository;
import pe.edu.upc.legalai.repositories.MensajesRepository;
import pe.edu.upc.legalai.repositories.SesionChatRepository;
import pe.edu.upc.legalai.servicesimplements.ChatAuditService;
import pe.edu.upc.legalai.servicesimplements.ChatServiceImpl;
import pe.edu.upc.legalai.servicesimplements.ChatStore;
import pe.edu.upc.legalai.servicesinterfaces.RAGService;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    final SesionChatRepository sesiones = mock(SesionChatRepository.class);
    final MensajesRepository mensajes = mock(MensajesRepository.class);
    final ExpedienteRepository expedientes = mock(ExpedienteRepository.class);
    final DocumentoRepository documentos = mock(DocumentoRepository.class);
    final UsuarioService usuarios = mock(UsuarioService.class);
    final RAGService rag = mock(RAGService.class);
    final ChatStore store = mock(ChatStore.class);
    final ChatAuditService auditoria = mock(ChatAuditService.class);

    final Usuario user = new Usuario();
    final Expediente expediente = new Expediente();
    final Documento documento = new Documento();

    ChatServiceImpl service;

    @BeforeEach
    void setup() {
        user.setUserId(7L);
        expediente.setCaseId(1L);
        documento.setDocumentId(5L);
        when(usuarios.obtenerUsuarioAutenticado()).thenReturn(user);
        when(expedientes.findByCaseIdAndOwnerUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(expedientes.findByCaseIdAndOwnerUserId(eq(1L), anyLong())).thenReturn(Optional.of(expediente));
        when(sesiones.save(any())).thenAnswer(call -> {
            SesionChat sesion = call.getArgument(0);
            sesion.setId(12L);
            sesion.setCreatedAt(LocalDateTime.now());
            sesion.setUpdatedAt(LocalDateTime.now());
            return sesion;
        });
        service = new ChatServiceImpl(sesiones, mensajes, expedientes, documentos, usuarios, rag, store, auditoria,
                new ChatSettings(10, 2000, 20, 100, "heuristic"));
    }

    SesionChat sesion(Long id, Long ownerId) {
        Usuario owner = new Usuario();
        owner.setUserId(ownerId);
        SesionChat sesion = new SesionChat();
        sesion.setId(id);
        sesion.setUsuario(owner);
        sesion.setExpediente(expediente);
        sesion.setTitulo("Titulo");
        sesion.setCreatedAt(LocalDateTime.now());
        sesion.setUpdatedAt(LocalDateTime.now());
        return sesion;
    }

    Mensajes mensaje(Long id, SenderType type, String content) {
        Mensajes mensaje = new Mensajes();
        mensaje.setMessageId(id);
        mensaje.setSenderType(type);
        mensaje.setContent(content);
        return mensaje;
    }

    ChatCreateSessionRequestDTO crear(Long caseId, Long documentId, String title) {
        ChatCreateSessionRequestDTO request = new ChatCreateSessionRequestDTO();
        request.setCaseId(caseId);
        request.setDocumentId(documentId);
        request.setTitle(title);
        return request;
    }

    ChatSendMessageRequestDTO mensaje(String content, String clientMessageId) {
        ChatSendMessageRequestDTO request = new ChatSendMessageRequestDTO();
        request.setContent(content);
        request.setClientMessageId(clientMessageId);
        return request;
    }

    RAGResponseDTO ragAnswer() {
        return new RAGResponseDTO("20 W [F1]", "gemini", "mock-model", 1, "CONSULTED_FRAGMENTS",
                List.of(new RAGSourceDTO("F1", 5L, "Falcon.pdf", 23L, 0, "Potencia 20 W", 0.24, 0, 12)));
    }

    void ragReady(Long documentId) {
        when(store.alcance(eq(12L), anyLong())).thenReturn(new ChatStore.Alcance(12L, 1L, documentId));
        when(store.historialReciente(eq(12L), anyInt(), anyLong())).thenReturn(List.of());
        when(store.guardarUsuario(eq(12L), anyString(), any()))
                .thenAnswer(call -> mensaje(101L, SenderType.USER, call.getArgument(1)));
        when(store.guardarAsistente(eq(12L), anyLong(), anyString(), any(RAGResponseDTO.class)))
                .thenReturn(mensaje(102L, SenderType.ASSISTANT, "20 W [F1]"));
        when(rag.preguntarExpedienteConversacional(eq(1L), any(), any())).thenReturn(ragAnswer());
        when(rag.preguntarDocumentoConversacional(eq(5L), any(), any())).thenReturn(ragAnswer());
    }

    @Test void createsSessionOwnedByTheAuthenticatedUser() {
        var response = service.crear(crear(1L, null, "  Analisis inicial  "));
        assertThat(response.sessionId()).isEqualTo(12L);
        assertThat(response.caseId()).isEqualTo(1L);
        assertThat(response.documentId()).isNull();
        assertThat(response.title()).isEqualTo("Analisis inicial");
        assertThat(response.messageCount()).isZero();
        var captor = org.mockito.ArgumentCaptor.forClass(SesionChat.class);
        verify(sesiones).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(user);
        assertThat(captor.getValue().getExpediente()).isSameAs(expediente);
        assertThat(captor.getValue().getDocumento()).isNull();
        verify(auditoria).registrar(user, "CREATE_CHAT_SESSION", 12L, "caseId=1; scope=Expediente");
    }

    @Test void createsSessionLimitedToAnOwnedDocument() {
        when(documentos.findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(eq(5L), eq(1L), eq(7L)))
                .thenReturn(Optional.of(documento));
        var response = service.crear(crear(1L, 5L, "Consulta tecnica Falcon"));
        assertThat(response.documentId()).isEqualTo(5L);
        assertThat(response.documentScopeRequired()).isTrue();
        verify(documentos).findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(5L, 1L, 7L);
        var captor = org.mockito.ArgumentCaptor.forClass(SesionChat.class);
        verify(sesiones).save(captor.capture());
        assertThat(captor.getValue().getDocumento()).isSameAs(documento);
        verify(auditoria).registrar(user, "CREATE_CHAT_SESSION", 12L, "caseId=1; scope=Documento");
    }

    @Test void defaultTitleIsAppliedWhenMissing() {
        assertThat(service.crear(crear(1L, null, null)).title()).isEqualTo("Nueva conversacion");
        assertThat(service.crear(crear(1L, null, "   ")).title()).isEqualTo("Nueva conversacion");
    }

    @Test void rejectsMissingOrForeignCase() {
        assertThatThrownBy(() -> service.crear(crear(null, null, "x"))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.crear(crear(0L, null, "x"))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.crear(crear(9L, null, "x"))).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(documentos, mensajes);
        verify(sesiones, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test void rejectsForeignOrOutOfScopeDocument() {
        when(documentos.findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        for (Long documentId : List.of(5L, 99L)) {
            assertThatThrownBy(() -> service.crear(crear(1L, documentId, "x")))
                    .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Documento no encontrado");
        }
        assertThatThrownBy(() -> service.crear(crear(1L, -1L, "x"))).isInstanceOf(BadRequestException.class);
        verify(sesiones, never()).save(any());
    }

    @Test void listsOnlyOwnSessionsNewestFirst() {
        when(sesiones.findByUsuario_UserIdOrderByUpdatedAtDescIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(sesion(12L, 7L)));
        when(mensajes.countBySesionId(12L)).thenReturn(4L);
        var page = service.listar(null, null, null);
        assertThat(page).hasSize(1);
        assertThat(page.getFirst().messageCount()).isEqualTo(4L);
        verify(sesiones).findByUsuario_UserIdOrderByUpdatedAtDescIdDesc(eq(7L),
                argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 20 && p.getSort().isUnsorted()));
        verify(sesiones, never()).findByUsuario_UserIdAndExpediente_CaseIdOrderByUpdatedAtDescIdDesc(
                anyLong(), anyLong(), any());
    }

    @Test void listsFilteredByOwnCaseAndRejectsForeignCase() {
        when(sesiones.findByUsuario_UserIdAndExpediente_CaseIdOrderByUpdatedAtDescIdDesc(
                eq(7L), eq(1L), any(Pageable.class))).thenReturn(List.of(sesion(12L, 7L)));
        assertThat(service.listar(1L, 1, 5)).hasSize(1);
        verify(sesiones).findByUsuario_UserIdAndExpediente_CaseIdOrderByUpdatedAtDescIdDesc(eq(7L), eq(1L),
                argThat(p -> p.getPageNumber() == 1 && p.getPageSize() == 5));
        assertThatThrownBy(() -> service.listar(9L, null, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void pageSizeIsClampedToTheConfiguredMaximum() {
        when(sesiones.findByUsuario_UserIdOrderByUpdatedAtDescIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(List.of());
        service.listar(null, -3, 5000);
        verify(sesiones).findByUsuario_UserIdOrderByUpdatedAtDescIdDesc(eq(7L),
                argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 100));
    }

    @Test void getsOwnSessionWithMessageCount() {
        when(sesiones.findById(12L)).thenReturn(Optional.of(sesion(12L, 7L)));
        when(mensajes.countBySesionId(12L)).thenReturn(2L);
        assertThat(service.obtener(12L).messageCount()).isEqualTo(2L);
    }

    @Test void forbidsReadingOrUpdatingAForeignSession() {
        when(sesiones.findById(12L)).thenReturn(Optional.of(sesion(12L, 8L)));
        assertThatThrownBy(() -> service.obtener(12L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.historial(12L, null, null)).isInstanceOf(ResourceNotFoundException.class);
        ChatUpdateSessionRequestDTO request = new ChatUpdateSessionRequestDTO();
        request.setTitle("Intento de renombrado ajeno");
        assertThatThrownBy(() -> service.actualizarTitulo(12L, request)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.actualizarTitulo(12L, new ChatUpdateSessionRequestDTO()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.eliminar(12L)).isInstanceOf(ResourceNotFoundException.class);
        when(sesiones.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(sesiones, never()).save(any());
        verify(sesiones, never()).delete(any());
        verifyNoInteractions(mensajes, rag);
    }

    @Test void returnsHistoryInChronologicalOrder() {
        when(sesiones.findById(12L)).thenReturn(Optional.of(sesion(12L, 7L)));
        when(mensajes.findBySesionIdOrderByCreatedAtAscMessageIdAsc(eq(12L), any(Pageable.class)))
                .thenReturn(List.of(mensaje(1L, SenderType.USER, "q1"), mensaje(2L, SenderType.ASSISTANT, "a1")));
        var history = service.historial(12L, 0, 50);
        assertThat(history).extracting(m -> m.messageId()).containsExactly(1L, 2L);
        assertThat(history).extracting(m -> m.role()).containsExactly("USER", "ASSISTANT");
        verify(mensajes).findBySesionIdOrderByCreatedAtAscMessageIdAsc(eq(12L),
                argThat(p -> p.getSort().isUnsorted()));
    }

    @Test void sendsMessageOverTheWholeCase() {
        ragReady(null);
        var response = service.enviarMensaje(12L, mensaje("  ¿Cual es la potencia de la Falcon A1 Pro?  ", null));
        assertThat(response.sessionId()).isEqualTo(12L);
        assertThat(response.userMessage().content()).isEqualTo("¿Cual es la potencia de la Falcon A1 Pro?");
        assertThat(response.userMessage().role()).isEqualTo("USER");
        assertThat(response.assistantMessage().role()).isEqualTo("ASSISTANT");
        assertThat(response.model()).isEqualTo("mock-model");
        assertThat(response.sourcesType()).isEqualTo("CONSULTED_FRAGMENTS");
        assertThat(response.sources()).hasSize(1);
        verify(rag).preguntarExpedienteConversacional(eq(1L), any(RAGRequestDTO.class), any());
        verify(rag, never()).preguntarDocumentoConversacional(anyLong(), any(), any());
        verify(store).guardarUsuario(eq(12L), eq("¿Cual es la potencia de la Falcon A1 Pro?"), eq(null));
        verify(auditoria).registrar(user, "SEND_CHAT_MESSAGE", 12L,
                "caseId=1; scope=Expediente; retrievedChunks=1; result=SUCCESS");
    }

    @Test void sendsMessageRestrictedToTheSessionDocument() {
        ragReady(5L);
        when(documentos.findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(eq(5L), eq(1L), eq(7L)))
                .thenReturn(Optional.of(documento));
        service.enviarMensaje(12L, mensaje("¿Y cual es su peso?", null));
        verify(rag).preguntarDocumentoConversacional(eq(5L), any(RAGRequestDTO.class), any());
        verify(rag, never()).preguntarExpedienteConversacional(anyLong(), any(), any());
        verify(auditoria).registrar(user, "SEND_CHAT_MESSAGE", 12L,
                "caseId=1; scope=Documento; retrievedChunks=1; result=SUCCESS");
    }

    @Test void theRoleIsAlwaysAssignedByTheBackend() {
        ragReady(null);
        service.enviarMensaje(12L, mensaje("pregunta", null));
        verify(rag).preguntarExpedienteConversacional(eq(1L), argThat(r -> r.getQuestion().equals("pregunta")
                && r.getTopK() == null), any());
        verify(store).guardarUsuario(eq(12L), eq("pregunta"), eq(null));
    }

    @Test void revalidatesScopeOnEveryMessage() {
        ragReady(5L);
        when(documentos.findByDocumentIdAndExpedienteCaseIdAndExpedienteOwnerUserId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(rag);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
    }

    @Test void doesNotWidenADeletedDocumentScopeToTheWholeCase() {
        when(store.alcance(12L, 7L)).thenReturn(new ChatStore.Alcance(12L, 1L, null, true));
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("pregunta", null)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("documento");
        verifyNoInteractions(rag);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
    }

    @Test void limitsConversationMemoryToTheConfiguredWindow() {
        ragReady(null);
        List<Mensajes> previous = List.of(
                mensaje(1L, SenderType.USER, "¿Cual es el plazo de pago?"),
                mensaje(2L, SenderType.ASSISTANT, "30 dias [F1]"),
                mensaje(3L, SenderType.USER, "¿Y si no se cumple?"),
                mensaje(4L, SenderType.ASSISTANT, "Se aplica penalidad [F1]"));
        when(store.historialReciente(eq(12L), anyInt(), anyLong())).thenReturn(previous);
        service.enviarMensaje(12L, mensaje("Menciona las obligaciones anteriores", null));
        verify(store).historialReciente(12L, 10, 101L);
        verify(rag).preguntarExpedienteConversacional(eq(1L), any(), argThat(historial ->
                historial.size() == 4 && "USER".equals(historial.getFirst().role())
                        && "ASSISTANT".equals(historial.get(1).role())));
    }

    @Test void refusesToWriteIntoAForeignSession() {
        when(store.alcance(eq(12L), anyLong())).thenThrow(new ResourceNotFoundException("Sesion no encontrada"));
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(rag);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
        verify(store, never()).historialReciente(anyLong(), anyInt(), anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsEmptyMessages(String content) {
        ragReady(null);
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje(content, null)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(rag);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
    }

    @Test void rejectsTooLongMessagesAndInvalidClientIds() {
        ragReady(null);
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("x".repeat(2001), null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("2000");
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", "no valido")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", "x".repeat(65))))
                .isInstanceOf(BadRequestException.class);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"service", "failure", "timeout", "rate-limit"})
    void keepsTheUserTurnButNeverInventsAnAssistantAnswer(String failure) {
        ragReady(null);
        RuntimeException error = switch (failure) {
            case "service" -> new IAServiceException();
            case "timeout" -> new IllegalStateException("timeout");
            case "rate-limit" -> new org.springframework.dao.QueryTimeoutException("429");
            default -> new RuntimeException("boom");
        };
        when(rag.preguntarExpedienteConversacional(anyLong(), any(), any())).thenThrow(error);
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("pregunta", null))).isSameAs(error);
        verify(store).guardarUsuario(eq(12L), eq("pregunta"), eq(null));
        verify(store, never()).guardarAsistente(anyLong(), anyLong(), anyString(), any());
        verify(auditoria).registrarFallo(user, "SEND_CHAT_MESSAGE", 12L,
                "caseId=1; scope=Expediente; result=FAILED; type=" + error.getClass().getSimpleName());
    }

    @Test void blankAnswerIsRejectedInsteadOfBeingPersisted() {
        ragReady(null);
        when(rag.preguntarExpedienteConversacional(anyLong(), any(), any()))
                .thenReturn(new RAGResponseDTO("   ", null, null, 0, "CONSULTED_FRAGMENTS", List.of()));
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", null)))
                .isInstanceOf(IAServiceException.class);
        when(rag.preguntarExpedienteConversacional(anyLong(), any(), any())).thenReturn(null);
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", null)))
                .isInstanceOf(IAServiceException.class);
        verify(store, never()).guardarAsistente(anyLong(), anyLong(), anyString(), any());
    }

    @Test void emptyRetrievalIsAnsweredWithoutAnyFictitiousSource() {
        ragReady(null);
        when(rag.preguntarExpedienteConversacional(anyLong(), any(), any()))
                .thenReturn(new RAGResponseDTO("No se encontro informacion documental suficiente para responder.",
                        null, null, 0, "CONSULTED_FRAGMENTS", List.of()));
        var response = service.enviarMensaje(12L, mensaje("q", null));
        assertThat(response.retrievedChunks()).isZero();
        assertThat(response.model()).isNull();
        assertThat(response.sources()).isEmpty();
        assertThat(response.assistantMessage().role()).isEqualTo("ASSISTANT");
    }

    @Test void auditFailureDoesNotHideTheProviderError() {
        ragReady(null);
        when(rag.preguntarExpedienteConversacional(anyLong(), any(), any())).thenThrow(new IAServiceException());
        doThrow(new IllegalStateException("audit down")).when(auditoria)
                .registrarFallo(any(), anyString(), anyLong(), anyString());
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("q", null)))
                .isInstanceOf(IAServiceException.class);
    }

    @Test void retriesWithTheSameClientMessageIdDoNotDuplicateAnything() {
        ragReady(null);
        Mensajes user = mensaje(101L, SenderType.USER, "pregunta");
        Mensajes assistant = mensaje(102L, SenderType.ASSISTANT, "20 W [F1]");
        when(store.porClientMessageId(12L, "turno-1")).thenReturn(Optional.of(user));
        when(store.respuestaDe(12L, 101L)).thenReturn(Optional.of(assistant));
        when(store.metadata(assistant)).thenReturn(new ChatTurnMetadata("gemini", "mock-model", 1,
                "CONSULTED_FRAGMENTS", ragAnswer().sources()));
        var response = service.enviarMensaje(12L, mensaje("pregunta", "turno-1"));
        assertThat(response.userMessage().messageId()).isEqualTo(101L);
        assertThat(response.assistantMessage().messageId()).isEqualTo(102L);
        assertThat(response.sources()).hasSize(1);
        assertThat(response.retrievedChunks()).isEqualTo(1);
        verifyNoInteractions(rag);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
        verifyNoInteractions(auditoria);
    }

    @Test void doesNotInventSourceMetadataForAnOlderStoredAnswer() {
        ragReady(null);
        Mensajes user = mensaje(101L, SenderType.USER, "pregunta");
        Mensajes assistant = mensaje(102L, SenderType.ASSISTANT, "20 W [F1]");
        when(store.porClientMessageId(12L, "turno-legacy")).thenReturn(Optional.of(user));
        when(store.respuestaDe(12L, 101L)).thenReturn(Optional.of(assistant));

        var response = service.enviarMensaje(12L, mensaje("pregunta", "turno-legacy"));

        assertThat(response.assistantMessage().content()).isEqualTo("20 W [F1]");
        assertThat(response.retrievedChunks()).isNull();
        assertThat(response.sourcesType()).isNull();
        assertThat(response.sources()).isNull();
        verifyNoInteractions(rag);
    }

    @Test void retryAfterAFailedTurnResumesWithoutLosingTheUserMessage() {
        ragReady(null);
        Mensajes user = mensaje(101L, SenderType.USER, "pregunta");
        when(store.porClientMessageId(12L, "turno-1")).thenReturn(Optional.of(user));
        when(store.respuestaDe(12L, 101L)).thenReturn(Optional.empty());
        service.enviarMensaje(12L, mensaje("pregunta", "turno-1"));
        verify(rag).preguntarExpedienteConversacional(eq(1L), any(), any());
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
        verify(store).guardarAsistente(eq(12L), eq(101L), eq("20 W [F1]"), any(RAGResponseDTO.class));
    }

    @Test void rejectsReusingAnIdempotencyKeyWithDifferentContent() {
        ragReady(null);
        Mensajes user = mensaje(101L, SenderType.USER, "pregunta original");
        when(store.porClientMessageId(12L, "turno-1")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.enviarMensaje(12L, mensaje("otra pregunta", "turno-1")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("otro contenido");
        verifyNoInteractions(rag);
        verify(store, never()).guardarUsuario(anyLong(), anyString(), any());
    }

    @Test void retriesWithoutClientMessageIdAlwaysCreateANewTurn() {
        ragReady(null);
        service.enviarMensaje(12L, mensaje("pregunta", "   "));
        verify(rag).preguntarExpedienteConversacional(eq(1L), any(), any());
        verify(store).guardarUsuario(eq(12L), eq("pregunta"), eq(null));
    }

    @Test void renamesOnlyTheTitle() {
        SesionChat propia = sesion(12L, 7L);
        Documento doc = new Documento();
        doc.setDocumentId(5L);
        propia.setDocumento(doc);
        when(sesiones.findById(12L)).thenReturn(Optional.of(propia));
        when(mensajes.countBySesionId(12L)).thenReturn(2L);
        ChatUpdateSessionRequestDTO request = new ChatUpdateSessionRequestDTO();
        request.setTitle("  Nuevo titulo  ");
        var response = service.actualizarTitulo(12L, request);
        assertThat(response.title()).isEqualTo("Nuevo titulo");
        assertThat(response.documentId()).isEqualTo(5L);
        assertThat(response.caseId()).isEqualTo(1L);
        verify(sesiones).save(propia);
    }

    @Test void deletesOnlyTheConversation() {
        when(sesiones.findById(12L)).thenReturn(Optional.of(sesion(12L, 7L)));
        service.eliminar(12L);
        verify(mensajes).deleteBySesionId(12L);
        verify(sesiones).delete(any(SesionChat.class));
        verify(sesiones, never()).deleteAll();
        verifyNoInteractions(documentos, expedientes);
        verify(auditoria).registrar(user, "DELETE_CHAT_SESSION", 12L, "caseId=1; messages=DELETED");
    }
}