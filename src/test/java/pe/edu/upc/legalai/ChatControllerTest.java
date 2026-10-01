package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.DTOs.response.ChatMessageDTO;
import pe.edu.upc.legalai.DTOs.response.ChatMessageResponseDTO;
import pe.edu.upc.legalai.DTOs.response.ChatSessionResponseDTO;
import pe.edu.upc.legalai.DTOs.response.RAGSourceDTO;
import pe.edu.upc.legalai.controllers.ChatController;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.ChatService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    final ChatService chat = mock(ChatService.class);
    final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ChatController(chat))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    ChatSessionResponseDTO sesion() {
        return new ChatSessionResponseDTO(12L, 1L, null, "Analisis inicial", 2L,
                LocalDateTime.parse("2026-10-03T10:00:00"), LocalDateTime.parse("2026-10-03T10:05:00"));
    }

    ChatMessageResponseDTO turno() {
        return new ChatMessageResponseDTO(12L,
                new ChatMessageDTO(101L, "USER", "¿Cual es el plazo?", LocalDateTime.parse("2026-10-03T10:00:00")),
                new ChatMessageDTO(102L, "ASSISTANT", "30 dias [F1]", LocalDateTime.parse("2026-10-03T10:00:04"),
                        "gemini", "gemini-2.5-flash", 1, "CONSULTED_FRAGMENTS",
                        List.of(new RAGSourceDTO("F1", 5L, "contrato.pdf", 23L, 4, "plazo de 30 dias", 0.21, 0, 12))),
                "gemini", "gemini-2.5-flash", 1, "CONSULTED_FRAGMENTS",
                List.of(new RAGSourceDTO("F1", 5L, "contrato.pdf", 23L, 4, "plazo de 30 dias", 0.21, 0, 12)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"caseId\":null}", "{\"caseId\":0}", "{\"caseId\":-1}",
            "{\"caseId\":1,\"documentId\":0}", "{\"caseId\":1,\"documentId\":-3}",
            "{\"caseId\":1,\"ownerUserId\":9}", "{\"caseId\":1,\"userId\":9}", "{\"caseId\":1,\"role\":\"ASSISTANT\"}",
            "{\"caseId\":1,\"documentoId\":5}", "{\"caseId\":\"abc\"}"})
    void rejectsInvalidSessionCreationBodies(String body) throws Exception {
        mvc.perform(post("/api/chat/sessions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chat);
    }

    @Test void rejectsBodiesThatExceedTheSizeLimits() throws Exception {
        for (String body : List.of("{\"caseId\":1,\"title\":\"" + "t".repeat(151) + "\"}",
                "{\"content\":\"" + "x".repeat(2001) + "\"}",
                "{\"content\":\"q\",\"clientMessageId\":\"" + "c".repeat(65) + "\"}",
                "{\"title\":\"" + "t".repeat(151) + "\"}")) {
            String path = body.contains("content") && !body.contains("title")
                    ? "/api/chat/sessions/12/messages" : "/api/chat/sessions";
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(chat);
    }

    @Test void acceptsMessagesAtTheConfiguredSizeLimit() throws Exception {
        when(chat.enviarMensaje(eq(12L), any())).thenReturn(turno());
        mvc.perform(post("/api/chat/sessions/12/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "x".repeat(2000) + "\",\"clientMessageId\":\"" + "c".repeat(64) + "\"}"))
                .andExpect(status().isOk());
        verify(chat).enviarMensaje(eq(12L), any());
    }

    @Test void createsASessionAndReturns201() throws Exception {
        when(chat.crear(any())).thenReturn(sesion());
        mvc.perform(post("/api/chat/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":1,\"title\":\"Analisis inicial\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(12))
                .andExpect(jsonPath("$.caseId").value(1))
                .andExpect(jsonPath("$.messageCount").value(2));
        verify(chat).crear(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"content\":null}", "{\"content\":\"\"}", "{\"content\":\"   \"}",
            "{\"content\":\"q\",\"role\":\"USER\"}", "{\"content\":\"q\",\"sessionId\":5}", "{\"content\":\"q\",\"senderType\":\"USER\"}",
            "{\"content\":\"q\",\"ownerUserId\":9}", "{\"content\":\"q\",\"messageId\":1}"})
    void rejectsInvalidMessageBodiesWithoutCallingTheService(String body) throws Exception {
        mvc.perform(post("/api/chat/sessions/12/messages").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chat);
    }

    @Test void acceptsAMessageWithoutClientMessageId() throws Exception {
        when(chat.enviarMensaje(eq(12L), any())).thenReturn(turno());
        mvc.perform(post("/api/chat/sessions/12/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Cual es el plazo?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(12))
                .andExpect(jsonPath("$.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.assistantMessage.sources[0].reference").value("F1"))
                .andExpect(jsonPath("$.sources[0].chunkId").value(23))
                .andExpect(jsonPath("$.model").value("gemini-2.5-flash"));
    }

    @Test void acceptsAnIdempotentMessage() throws Exception {
        when(chat.enviarMensaje(eq(12L), any())).thenReturn(turno());
        mvc.perform(post("/api/chat/sessions/12/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Cual es el plazo?\",\"clientMessageId\":\"turno-1\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null}", "{\"title\":\"\"}", "{\"title\":\"   \"}",
            "{\"title\":\"ok\",\"content\":\"x\"}",
            "{\"title\":\"ok\",\"caseId\":9}", "{\"title\":\"ok\",\"documentId\":9}"})
    void onlyTheTitleCanBeUpdated(String body) throws Exception {
        mvc.perform(patch("/api/chat/sessions/12").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chat);
    }

    @Test void updatesTheTitle() throws Exception {
        when(chat.actualizarTitulo(eq(12L), any())).thenReturn(sesion());
        mvc.perform(patch("/api/chat/sessions/12").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo titulo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Analisis inicial"));
    }

    @Test void listsSessionsWithOptionalFiltersAndPagination() throws Exception {
        when(chat.listar(any(), any(), any())).thenReturn(List.of(sesion()));
        mvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(12));
        verify(chat).listar(null, null, null);
        mvc.perform(get("/api/chat/sessions").param("caseId", "1").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
        verify(chat).listar(1L, 0, 10);
        verifyNoMoreInteractionsOnList();
    }

    void verifyNoMoreInteractionsOnList() {
        org.mockito.Mockito.verifyNoMoreInteractions(chat);
    }

    @Test void readsHistoryAndSessionDetail() throws Exception {
        when(chat.obtener(12L)).thenReturn(sesion());
        mvc.perform(get("/api/chat/sessions/12")).andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").doesNotExist());
        when(chat.historial(eq(12L), any(), any())).thenReturn(List.of(
                new ChatMessageDTO(101L, "USER", "q", LocalDateTime.parse("2026-10-03T10:00:00")),
                new ChatMessageDTO(102L, "ASSISTANT", "a", LocalDateTime.parse("2026-10-03T10:00:04"))));
        mvc.perform(get("/api/chat/sessions/12/messages").param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"));
        verify(chat).historial(12L, 0, 50);
    }

    @Test void deletesASessionWithNoContent() throws Exception {
        mvc.perform(delete("/api/chat/sessions/12")).andExpect(status().isNoContent());
        verify(chat).eliminar(12L);
    }

    @Test void mapsDomainErrorsToTheExpectedStatus() throws Exception {
        when(chat.obtener(99L)).thenThrow(new ResourceNotFoundException("Sesion no encontrada"));
        mvc.perform(get("/api/chat/sessions/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sesion no encontrada"));

        when(chat.actualizarTitulo(eq(12L), any())).thenThrow(new BadRequestException("Titulo invalido"));
        mvc.perform(patch("/api/chat/sessions/12").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\"}"))
                .andExpect(status().isBadRequest());

        when(chat.enviarMensaje(eq(12L), any())).thenThrow(new IAServiceException());
        mvc.perform(post("/api/chat/sessions/12/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"q\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inteligencia artificial")));
    }

    @Test void rejectsNonNumericIdentifiers() throws Exception {
        mvc.perform(get("/api/chat/sessions/abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(chat);
    }
}