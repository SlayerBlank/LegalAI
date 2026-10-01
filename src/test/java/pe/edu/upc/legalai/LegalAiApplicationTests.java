package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import pe.edu.upc.legalai.securities.JwtTokenUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LegalAiApplicationTests {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private pe.edu.upc.legalai.servicesinterfaces.EmbeddingService embeddings;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private pe.edu.upc.legalai.servicesinterfaces.IAService ia;

    @Test
    void ragUsesScopedRetrievalAndAuditsSuccessAndFailureWithoutConversation() throws Exception {
        JsonNode auth = register();
        String token = auth.path("accessToken").asText();
        long userId = auth.path("user").path("userId").asLong();
        String foreign = register().path("accessToken").asText();
        long caseId = json(call("POST", "/api/cases", token,
                Map.of("clientId",createClient(token),"title","RAG case"),201)).path("caseId").asLong();
        long otherCase = json(call("POST", "/api/cases", token,
                Map.of("clientId",createClient(token),"title","Excluded RAG case"),201)).path("caseId").asLong();
        long foreignCase = json(call("POST", "/api/cases", foreign,
                Map.of("clientId",createClient(foreign),"title","Foreign RAG case"),201)).path("caseId").asLong();
        long doc = embeddingDocument(token,caseId);
        long second = embeddingDocument(token,caseId);
        long excluded = embeddingDocument(token,otherCase);
        long foreignDoc = embeddingDocument(foreign,foreignCase);
        var question = Map.of("question","private-question","topK",5);
        String endpoint = "/api/documents/"+doc+"/ask";
        for (String path : new String[]{endpoint,"/api/cases/"+caseId+"/ask"}) {
            call("POST",path,null,question,401);
            call("POST",path,"invalid",question,401);
            call("POST",path,foreign,question,404);
        }
        call("POST","/api/documents/9223372036854775807/ask",token,question,404);
        call("POST","/api/cases/9223372036854775807/ask",token,question,404);
        call("POST","/api/documents/"+foreignDoc+"/ask",token,question,404);
        org.mockito.Mockito.verifyNoInteractions(ia,embeddings);
        float[] vector = new float[768]; vector[0]=1;
        org.mockito.Mockito.when(embeddings.generarEmbeddingConsulta(org.mockito.ArgumentMatchers.anyString())).thenReturn(vector);
        JsonNode empty = json(call("POST",endpoint,token,question,200));
        assertThat(empty.path("retrievedChunks").asInt()).isZero(); // No chunks.
        for (long id : new long[]{doc,second,excluded,foreignDoc}) jdbc.update("""
                insert into document_chunks(document_id,chunk_index,content,char_start,char_end,created_at)
                values (?,0,?,0,30,current_timestamp)
                """,id,"private-source-for-document-"+id);
        assertThat(json(call("POST",endpoint,token,question,200)).path("sources").size()).isZero(); // No embeddings.
        org.mockito.Mockito.verifyNoInteractions(ia);
        for (long id : new long[]{doc,second,excluded,foreignDoc}) jdbc.update("""
                update document_chunks set embedding=cast(? as vector),embedding_model='gemini-embedding-001' where document_id=?
                """,java.util.Arrays.toString(vector),id);
        var answer = new pe.edu.upc.legalai.DTOs.response.IAResponseDTO();
        answer.setAnswer("private-answer [F1]"); answer.setProvider("gemini"); answer.setModel("mock-model");
        org.mockito.Mockito.when(ia.generarRespuestaDocumental(org.mockito.ArgumentMatchers.any())).thenReturn(answer);
        JsonNode result = json(call("POST",endpoint,token,question,200));
        assertThat(result.path("model").asText()).isEqualTo("mock-model");
        assertThat(result.path("sourcesType").asText()).isEqualTo("CONSULTED_FRAGMENTS");
        assertThat(result.path("sources").size()).isEqualTo(1);
        assertThat(result.path("sources").get(0).path("documentId").asLong()).isEqualTo(doc);
        assertThat(result.path("sources").get(0).path("documentName").asText()).isEqualTo("retrieval.pdf");
        var sent = org.mockito.ArgumentCaptor.forClass(pe.edu.upc.legalai.DTOs.request.IAContextRequestDTO.class);
        org.mockito.Mockito.verify(ia).generarRespuestaDocumental(sent.capture());
        assertThat(objectMapper.readTree(sent.getValue().context()).size()).isEqualTo(1);
        org.mockito.Mockito.clearInvocations(ia,embeddings);
        result = json(call("POST","/api/cases/"+caseId+"/ask",token,question,200));
        assertThat(result.path("sources").size()).isEqualTo(2);
        for (JsonNode source : result.path("sources")) assertThat(source.path("documentId").asLong()).isIn(doc,second);
        org.mockito.Mockito.verify(ia).generarRespuestaDocumental(sent.capture());
        for (JsonNode source : objectMapper.readTree(sent.getValue().context()))
            assertThat(source.path("documentId").asLong()).isIn(doc,second);
        org.mockito.Mockito.verify(embeddings).generarEmbeddingConsulta("private-question");
        org.mockito.Mockito.verify(embeddings,org.mockito.Mockito.never()).generarEmbeddings(org.mockito.ArgumentMatchers.anyList());
        org.mockito.Mockito.verify(embeddings,org.mockito.Mockito.never()).generarEmbedding(org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.doThrow(new pe.edu.upc.legalai.exceptions.IAServiceException()).when(ia)
                .generarRespuestaDocumental(org.mockito.ArgumentMatchers.any());
        call("POST",endpoint,token,question,503);
        var logs = jdbc.queryForList("select details from audit_logs where user_id=? and action='RAG_QUERY' order by log_id",String.class,userId);
        assertThat(logs).anyMatch(s -> s.contains("result=SUCCESS")).anyMatch(s -> s.contains("result=FAILED"));
        assertThat(logs).allMatch(s -> !s.contains("private-question") && !s.contains("private-answer") && !s.contains("private-source"));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id=? and action='RAG_QUERY' and created_at is null",Long.class,userId)).isZero();
        var api=json(call("GET","/v3/api-docs",null,null,200));
        for (String path : new String[]{"/api/documents/{documentId}/ask","/api/cases/{caseId}/ask","/api/ai/test",
                "/api/documents/{documentId}/search","/api/documents/{documentId}/embeddings"})
            assertThat(api.path("paths").path(path).has("post")).isTrue();
        org.mockito.Mockito.when(ia.generarRespuesta(org.mockito.ArgumentMatchers.any())).thenReturn(answer);
        assertThat(json(call("POST","/api/ai/test",token,Map.of("prompt","independent prompt"),200)).path("answer").asText()).isEqualTo("private-answer [F1]");
    }

    @Test
    void embeddingsAreAtomicOwnedAndSearchIsOrderedAndScoped() throws Exception {
        String token = register().path("accessToken").asText();
        String foreign = register().path("accessToken").asText();
        long caseId = json(call("POST", "/api/cases", token,
                Map.of("clientId", createClient(token), "title", "Retrieval test"), 201)).path("caseId").asLong();
        long otherCase = json(call("POST", "/api/cases", token,
                Map.of("clientId", createClient(token), "title", "Excluded case"), 201)).path("caseId").asLong();
        long doc = embeddingDocument(token, caseId);
        long secondDoc = embeddingDocument(token, caseId);
        long excludedDoc = embeddingDocument(token, otherCase);
        String base = "/api/documents/" + doc;
        var query = Map.of("query", "garantia", "topK", 5);
        for (String path : new String[]{base + "/embeddings", base + "/search", "/api/cases/" + caseId + "/search"}) {
            call("POST", path, null, query, 401);
            call("POST", path, "invalid", query, 401);
            call("POST", path, foreign, query, 404);
        }
        call("POST", "/api/documents/9223372036854775807/embeddings", token, null, 404);
        call("POST", "/api/documents/9223372036854775807/search", token, query, 404);
        call("POST", "/api/cases/9223372036854775807/search", token, query, 404);
        call("POST", base + "/embeddings", token, null, 400);
        org.mockito.Mockito.verifyNoInteractions(embeddings);
        for (long id : new long[]{doc, secondDoc, excludedDoc}) {
            for (int index = 0; index < 2; index++) jdbc.update("""
                    insert into document_chunks(document_id,chunk_index,content,char_start,char_end,created_at)
                    values (?,?,?,0,10,current_timestamp)
                    """, id, index, "chunk-" + index);
        }
        float[] near = new float[768]; near[0] = 1;
        float[] far = new float[768]; far[1] = 1;
        org.mockito.Mockito.when(embeddings.generarEmbeddingConsulta(org.mockito.ArgumentMatchers.anyString())).thenReturn(near);
        org.mockito.Mockito.when(embeddings.generarEmbeddings(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(inv -> java.util.List.of(((java.util.List<?>)inv.getArgument(0)).getFirst().equals("chunk-0") ? far : near));
        for (long id : new long[]{doc, secondDoc, excludedDoc}) {
            JsonNode result = json(call("POST", "/api/documents/" + id + "/embeddings", token, null, 200));
            assertThat(result.path("embeddingsGenerated").asInt()).isEqualTo(2);
        }
        org.mockito.Mockito.clearInvocations(embeddings);
        JsonNode skipped = json(call("POST", base + "/embeddings", token, null, 200));
        assertThat(skipped.path("chunksProcessed").asInt()).isEqualTo(2);
        assertThat(skipped.path("embeddingsGenerated").asInt()).isZero();
        org.mockito.Mockito.verifyNoInteractions(embeddings);
        assertThat(json(call("POST", base + "/embeddings?force=true", token, null, 200))
                .path("embeddingsGenerated").asInt()).isEqualTo(2);
        JsonNode results = json(call("POST", base + "/search", token, query, 200));
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).path("chunkIndex").asInt()).isEqualTo(1);
        assertThat(results.get(0).path("distance").asDouble()).isCloseTo(0, org.assertj.core.data.Offset.offset(0.00001));
        assertThat(results.get(1).path("distance").asDouble()).isCloseTo(1, org.assertj.core.data.Offset.offset(0.00001));
        for (JsonNode r : results) assertThat(r.path("documentId").asLong()).isEqualTo(doc);
        results = json(call("POST", "/api/cases/" + caseId + "/search", token, query, 200));
        assertThat(results.size()).isEqualTo(4);
        for (JsonNode r : results) assertThat(r.path("documentId").asLong()).isIn(doc, secondDoc);
        assertThat(json(call("POST", "/api/cases/" + caseId + "/search", token, Map.of("query","q","topK",1),200)).size()).isEqualTo(1);

        var nativeRepository = new pe.edu.upc.legalai.repositories.ChunkEmbeddingRepository(jdbc,
                new pe.edu.upc.legalai.config.EmbeddingSettings("gemini-embedding-001",768,1));
        assertThat(nativeRepository.searchDocument(doc, -1L, near, 5)).isEmpty();
        assertThat(nativeRepository.searchCase(caseId, -1L, near, 5)).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new pe.edu.upc.legalai.repositories.ChunkEmbeddingRepository(jdbc,
                new pe.edu.upc.legalai.config.EmbeddingSettings("gemini-embedding-001",1536,1)).validateSchema())
                .isInstanceOf(pe.edu.upc.legalai.exceptions.EmbeddingException.class);
        jdbc.update("update document_chunks set embedding=null, embedding_model=null where document_id=? and chunk_index=0",doc);
        org.mockito.Mockito.clearInvocations(embeddings);
        assertThat(json(call("POST", base + "/embeddings", token, null, 200)).path("embeddingsGenerated").asInt()).isEqualTo(1);
        org.mockito.Mockito.verify(embeddings,org.mockito.Mockito.times(1)).generarEmbeddings(java.util.List.of("chunk-0"));

        var before = jdbc.queryForList("select embedding::text from document_chunks where document_id=? order by chunk_index", String.class, doc);
        org.mockito.Mockito.doReturn(java.util.List.of(near))
                .doThrow(new pe.edu.upc.legalai.exceptions.EmbeddingException("simulated 429"))
                .when(embeddings).generarEmbeddings(org.mockito.ArgumentMatchers.anyList());
        assertThat(json(call("POST", base + "/embeddings?force=true", token, null, 503)).path("message").asText())
                .contains("chunks afectados", "Operacion revertida");
        assertThat(jdbc.queryForList("select embedding::text from document_chunks where document_id=? order by chunk_index", String.class, doc)).isEqualTo(before);
        org.mockito.Mockito.reset(embeddings);
        org.mockito.Mockito.when(embeddings.generarEmbeddings(org.mockito.ArgumentMatchers.anyList())).thenReturn(java.util.List.of(new float[]{1,2}));
        call("POST", base + "/embeddings?force=true", token, null, 503);
        assertThat(jdbc.queryForList("select embedding::text from document_chunks where document_id=? order by chunk_index", String.class, doc)).isEqualTo(before);
        org.mockito.Mockito.when(embeddings.generarEmbeddingConsulta(org.mockito.ArgumentMatchers.anyString())).thenReturn(new float[]{1,2});
        call("POST", base + "/search", token, query, 503);
        JsonNode api = json(call("GET", "/v3/api-docs", null, null, 200));
        for (String path : new String[]{"/api/documents/{documentId}/embeddings", "/api/documents/{documentId}/search", "/api/cases/{caseId}/search"})
            assertThat(api.path("paths").path(path).has("post")).isTrue();
    }

    @Test
    void chatSessionsAreScopedOrderedIdempotentAndAudited() throws Exception {
        JsonNode auth = register();
        String token = auth.path("accessToken").asText();
        long userId = auth.path("user").path("userId").asLong();
        String foreign = register().path("accessToken").asText();
        long caseId = json(call("POST", "/api/cases", token,
                Map.of("clientId", createClient(token), "title", "Chat case"), 201)).path("caseId").asLong();
        long otherCase = json(call("POST", "/api/cases", token,
                Map.of("clientId", createClient(token), "title", "Other chat case"), 201)).path("caseId").asLong();
        long documentId = chatDocument(token, caseId, "chat.pdf");
        long otherDocumentId = chatDocument(token, otherCase, "other.pdf");
        long foreignCase = json(call("POST", "/api/cases", foreign,
                Map.of("clientId", createClient(foreign), "title", "Foreign chat case"), 201)).path("caseId").asLong();
        long foreignDocumentId = chatDocument(foreign, foreignCase, "foreign.pdf");

        Map<String, Object> creation = Map.of("caseId", caseId, "title", "Conversacion inicial");
        call("POST", "/api/chat/sessions", null, creation, 401);
        call("POST", "/api/chat/sessions", "invalid", creation, 401);
        call("POST", "/api/chat/sessions", token, Map.of("caseId", caseId, "ownerUserId", userId), 400);
        call("POST", "/api/chat/sessions", token, Map.of("caseId", caseId, "documentId", foreignDocumentId), 404);
        call("POST", "/api/chat/sessions", token, Map.of("caseId", caseId, "documentId", otherDocumentId), 404);
        call("POST", "/api/chat/sessions", token, Map.of("caseId", Long.MAX_VALUE), 404);
        org.mockito.Mockito.verifyNoInteractions(ia, embeddings);

        JsonNode created = json(call("POST", "/api/chat/sessions", token, creation, 201));
        long sessionId = created.path("sessionId").asLong();
        assertThat(created.path("caseId").asLong()).isEqualTo(caseId);
        assertThat(created.path("documentId").isNull()).isTrue();
        assertThat(created.path("messageCount").asLong()).isZero();
        assertThat(jdbc.queryForObject("select user_id from chat_sessions where session_id=?", Long.class, sessionId))
                .isEqualTo(userId);
        assertThat(jdbc.queryForObject("select document_id from chat_sessions where session_id=?", Long.class, sessionId))
                .isNull();
        long documentSession = json(call("POST", "/api/chat/sessions", token,
                Map.of("caseId", caseId, "documentId", documentId, "title", "Solo documento"), 201))
                .path("sessionId").asLong();
        JsonNode documentSessionData = json(call("GET", "/api/chat/sessions/" + documentSession, token, null, 200));
        assertThat(documentSessionData.path("documentId").asLong()).isEqualTo(documentId);
        assertThat(documentSessionData.path("documentScopeRequired").asBoolean()).isTrue();

        call("GET", "/api/chat/sessions/" + sessionId, foreign, null, 404);
        call("GET", "/api/chat/sessions/" + sessionId + "/messages", foreign, null, 404);
        call("POST", "/api/chat/sessions/" + sessionId + "/messages", foreign, Map.of("content", "hola"), 404);
        call("PATCH", "/api/chat/sessions/" + sessionId, foreign, Map.of("title", "ajeno"), 404);
        call("DELETE", "/api/chat/sessions/" + sessionId, foreign, null, 404);
        call("GET", "/api/chat/sessions/" + Long.MAX_VALUE, token, null, 404);
        assertThat(json(call("GET", "/api/chat/sessions", foreign, null, 200))).isEmpty();
        call("GET", "/api/chat/sessions", null, null, 401);
        call("GET", "/api/chat/sessions?caseId=" + foreignCase, token, null, 404);
        assertThat(json(call("GET", "/api/chat/sessions?caseId=" + caseId, token, null, 200))).hasSize(2);

        float[] vector = new float[768];
        vector[0] = 1;
        org.mockito.Mockito.when(embeddings.generarEmbeddingConsulta(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(vector);
        for (long id : new long[]{documentId, otherDocumentId}) {
            jdbc.update("insert into document_chunks(document_id,chunk_index,content,char_start,char_end,created_at)"
                    + " values (?,0,?,0,40,current_timestamp)", id, "fuente privada del documento " + id);
            jdbc.update("update document_chunks set embedding=cast(? as vector),embedding_model='gemini-embedding-001'"
                    + " where document_id=?", java.util.Arrays.toString(vector), id);
        }
        var answer = new pe.edu.upc.legalai.DTOs.response.IAResponseDTO();
        answer.setAnswer("La potencia optica es 20 W [F1]");
        answer.setProvider("gemini");
        answer.setModel("mock-model");
        org.mockito.Mockito.when(ia.generarRespuestaDocumental(org.mockito.ArgumentMatchers.any())).thenReturn(answer);

        String endpoint = "/api/chat/sessions/" + sessionId + "/messages";
        call("POST", endpoint, token, Map.of("content", " "), 400);
        call("POST", endpoint, token, Map.of("content", "q", "clientMessageId", "x".repeat(65)), 400);
        call("POST", endpoint, token, Map.of("content", "q", "role", "ASSISTANT"), 400);
        JsonNode turn = json(call("POST", endpoint, token,
                Map.of("content", "Cual es la potencia optica?", "clientMessageId", "turno-1"), 200));
        assertThat(turn.path("sessionId").asLong()).isEqualTo(sessionId);
        assertThat(turn.path("userMessage").path("role").asText()).isEqualTo("USER");
        assertThat(turn.path("assistantMessage").path("role").asText()).isEqualTo("ASSISTANT");
        assertThat(turn.path("assistantMessage").path("sources").size()).isEqualTo(1);
        assertThat(turn.path("provider").asText()).isEqualTo("gemini");
        assertThat(turn.path("model").asText()).isEqualTo("mock-model");
        assertThat(turn.path("retrievedChunks").asInt()).isEqualTo(1);
        assertThat(turn.path("sourcesType").asText()).isEqualTo("CONSULTED_FRAGMENTS");
        assertThat(turn.path("sources").size()).isEqualTo(1);
        assertThat(turn.path("sources").get(0).path("documentId").asLong()).isIn(documentId, otherDocumentId);

        var rows = jdbc.queryForList("select message_id,sender_type,client_message_id,reply_to_message_id from chat_messages"
                + " where session_id=? order by message_id", sessionId);
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().get("sender_type")).isEqualTo("USER");
        assertThat(rows.getFirst().get("client_message_id")).isEqualTo("turno-1");
        assertThat(rows.getLast().get("sender_type")).isEqualTo("ASSISTANT");
        assertThat(rows.getLast().get("client_message_id")).isNull();
        assertThat(rows.getLast().get("reply_to_message_id")).isEqualTo(rows.getFirst().get("message_id"));
        assertThat(rows.getFirst().get("message_id")).isNotEqualTo(rows.getLast().get("message_id"));
        String storedMetadata = jdbc.queryForObject(
                "select response_metadata from chat_messages where message_id=?", String.class,
                rows.getLast().get("message_id"));
        assertThat(storedMetadata).contains("CONSULTED_FRAGMENTS", "F1", "documentName");
        assertThat(jdbc.queryForObject("select count(*) from chat_messages where session_id=?", Long.class, documentSession))
                .isZero();

        JsonNode retry = json(call("POST", endpoint, token,
                Map.of("content", "Cual es la potencia optica?", "clientMessageId", "turno-1"), 200));
        assertThat(retry.path("sources").size()).isEqualTo(1);
        assertThat(retry.path("assistantMessage").path("sources").size()).isEqualTo(1);
        assertThat(retry.path("userMessage").path("messageId").asLong())
                .isEqualTo(turn.path("userMessage").path("messageId").asLong());
        call("POST", endpoint, token, Map.of("content", "pregunta diferente", "clientMessageId", "turno-1"), 400);
        assertThat(jdbc.queryForObject("select count(*) from chat_messages where session_id=?", Long.class, sessionId))
                .isEqualTo(2L);

        JsonNode history = json(call("GET", endpoint, token, null, 200));
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).path("messageId").asLong())
                .isLessThan(history.get(1).path("messageId").asLong());
        assertThat(history.get(0).path("createdAt").asText())
                .isLessThanOrEqualTo(history.get(1).path("createdAt").asText());
        assertThat(history.get(1).path("sources").size()).isEqualTo(1);
        assertThat(json(call("GET", endpoint + "?page=0&size=1", token, null, 200)).size()).isEqualTo(1);

        org.mockito.Mockito.clearInvocations(embeddings, ia);
        call("POST", endpoint, token, Map.of("content", "Y cual es su peso?"), 200);
        var retrieval = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(embeddings, org.mockito.Mockito.atLeastOnce())
                .generarEmbeddingConsulta(retrieval.capture());
        assertThat(retrieval.getAllValues()).anyMatch(query ->
                query.contains("Cual es la potencia optica?") && query.contains("Y cual es su peso?"));
        var context = org.mockito.ArgumentCaptor.forClass(pe.edu.upc.legalai.DTOs.request.IAContextRequestDTO.class);
        org.mockito.Mockito.verify(ia).generarRespuestaDocumental(context.capture());
        assertThat(context.getValue().historial()).hasSize(2);
        assertThat(context.getValue().historial().getFirst().role()).isEqualTo("USER");
        assertThat(context.getValue().historial().getFirst().content()).isEqualTo("Cual es la potencia optica?");
        assertThat(context.getValue().historial().get(1).role()).isEqualTo("ASSISTANT");
        assertThat(context.getValue().question()).isEqualTo("Y cual es su peso?");

        org.mockito.Mockito.clearInvocations(ia, embeddings);
        String documentEndpoint = "/api/chat/sessions/" + documentSession + "/messages";
        JsonNode documentTurn = json(call("POST", documentEndpoint, token,
                Map.of("content", "Resume el contrato"), 200));
        assertThat(documentTurn.path("sources").size()).isEqualTo(1);
        assertThat(documentTurn.path("sources").get(0).path("documentId").asLong()).isEqualTo(documentId);
        assertThat(jdbc.queryForObject("select count(*) from chat_messages where session_id=?", Long.class, documentSession))
                .isEqualTo(2L);

        org.mockito.Mockito.doThrow(new pe.edu.upc.legalai.exceptions.IAServiceException()).when(ia)
                .generarRespuestaDocumental(org.mockito.ArgumentMatchers.any());
        call("POST", endpoint, token, Map.of("content", "Pregunta que fallara", "clientMessageId", "turno-fallido"), 503);
        assertThat(jdbc.queryForList("select sender_type from chat_messages where session_id=? order by message_id", sessionId))
                .extracting(row -> row.get("sender_type")).containsExactly("USER", "ASSISTANT", "USER", "ASSISTANT", "USER");
        org.mockito.Mockito.doReturn(answer).when(ia)
                .generarRespuestaDocumental(org.mockito.ArgumentMatchers.any());
        JsonNode resumed = json(call("POST", endpoint, token,
                Map.of("content", "Pregunta que fallara", "clientMessageId", "turno-fallido"), 200));
        assertThat(resumed.path("userMessage").path("content").asText()).isEqualTo("Pregunta que fallara");
        assertThat(jdbc.queryForObject("select count(*) from chat_messages where session_id=?", Long.class, sessionId))
                .isEqualTo(6L);

        var sessionRows = jdbc.queryForList("select details from audit_logs where user_id=? and action='CREATE_CHAT_SESSION'",
                String.class, userId);
        assertThat(sessionRows).hasSize(2)
                .anyMatch(details -> details.equals("caseId=" + caseId + "; scope=Expediente"))
                .anyMatch(details -> details.equals("caseId=" + caseId + "; scope=Documento"));
        assertThat(jdbc.queryForList("select details from audit_logs where user_id=? and action='SEND_CHAT_MESSAGE'",
                String.class, userId)).hasSize(5)
                .anyMatch(details -> details.contains("result=SUCCESS"))
                .anyMatch(details -> details.contains("result=FAILED"))
                .allMatch(details -> !details.contains("potencia") && !details.contains("20 W")
                        && !details.contains("turno-1"));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id=? and action='SEND_CHAT_MESSAGE'"
                + " and created_at is null", Long.class, userId)).isZero();

        JsonNode renamed = json(call("PATCH", "/api/chat/sessions/" + sessionId, token,
                Map.of("title", "Titulo actualizado"), 200));
        assertThat(renamed.path("title").asText()).isEqualTo("Titulo actualizado");
        call("PATCH", "/api/chat/sessions/" + sessionId, token, Map.of("title", " "), 400);
        JsonNode list = json(call("GET", "/api/chat/sessions?caseId=" + caseId, token, null, 200));
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).path("sessionId").asLong()).isEqualTo(sessionId);
        assertThat(list.get(1).path("sessionId").asLong()).isEqualTo(documentSession);

        assertThat(jdbc.queryForObject("select count(*) from document_chunks where document_id=?", Long.class, documentId))
                .isEqualTo(1L);
        assertThat(call("DELETE", "/api/chat/sessions/" + sessionId, token, null, 204).body()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from chat_messages where session_id=?", Long.class, sessionId))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from chat_sessions where session_id=?", Long.class, sessionId))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from documents where document_id=?", Long.class, documentId))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from document_chunks where document_id=?", Long.class, documentId))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id=? and action='DELETE_CHAT_SESSION'",
                Long.class, userId)).isEqualTo(1L);
        call("GET", "/api/chat/sessions/" + sessionId, token, null, 404);

        jdbc.update("delete from document_chunks where document_id=?", documentId);
        jdbc.update("delete from document_chunks where document_id=?", otherDocumentId);
        JsonNode docs = json(call("GET", "/v3/api-docs", null, null, 200));
        assertThat(docs.path("paths").has("/api/chat/sessions")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions").has("post")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions").has("get")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions/{sessionId}").has("patch")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions/{sessionId}").has("delete")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions/{sessionId}/messages").has("post")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions/{sessionId}/messages").has("get")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions").path("post").path("responses").has("201")).isTrue();
        assertThat(docs.path("paths").path("/api/chat/sessions/{sessionId}").path("delete")
                .path("responses").has("204")).isTrue();
    }

    private long chatDocument(String token, long caseId, String fileName) throws Exception {
        return json(call("POST", "/api/cases/" + caseId + "/documents", token,
                Map.of("fileName", fileName, "category", "CONTRACT"), 201)).path("documentId").asLong();
    }

    private long embeddingDocument(String token, long caseId) throws Exception {
        return json(call("POST", "/api/cases/" + caseId + "/documents", token,
                Map.of("fileName", "retrieval.pdf", "category", "CONTRACT"), 201)).path("documentId").asLong();
    }

    @org.junit.jupiter.api.io.TempDir
    static java.nio.file.Path uploadStorage;

    @org.springframework.test.context.DynamicPropertySource
    static void uploadProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("legalai.storage.path", () -> uploadStorage.toString());
        registry.add("legalai.documents.max-file-size", () -> "1KB");
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final HttpClient http = HttpClient.newHttpClient();
    private static final String PASSWORD = "Legal-test-2026!";
    private final java.util.List<Long> createdUsers = new java.util.ArrayList<>();

    @AfterEach
    void removeTestData() {
        for (Long userId : createdUsers) {
            jdbc.update("delete from documents where uploaded_by_user_id = ?", userId);
            jdbc.update("delete from cases where owner_user_id = ?", userId);
            jdbc.update("delete from clients where owner_user_id = ?", userId);
            jdbc.update("delete from audit_logs where user_id = ?", userId);
            jdbc.update("delete from users where user_id = ?", userId);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void usuarioAndRolPersistAndExposeOnlyPublicData() throws Exception {
        JsonNode auth = register();
        String token = auth.path("accessToken").asText();
        JsonNode user = auth.path("user");
        assertThat(user.path("userId").asLong()).isPositive();
        assertThat(user.path("createdAt").asText()).isNotBlank();
        assertThat(user.has("passwordHash")).isFalse();
        String hash = jdbc.queryForObject("select password_hash from users where user_id = ?",
                String.class, user.path("userId").asLong());
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
        JsonNode me = json(call("GET", "/api/users/me", token, null, 200));
        assertThat(me.path("userId")).isEqualTo(user.path("userId"));
        JsonNode roles = json(call("GET", "/api/roles", token, null, 200));
        assertThat(roles.toString()).contains("USER");
        call("GET", "/swagger-ui/index.html", null, null, 200);
        JsonNode docs = json(call("GET", "/v3/api-docs", null, null, 200));
        assertThat(docs.path("paths").has("/api/users/me")).isTrue();
        assertThat(docs.path("paths").has("/api/roles")).isTrue();
    }

    @Test
    void authRejectsInvalidCredentialsDuplicateEmailsAndOversizedPasswords() throws Exception {
        JsonNode auth = register();
        String email = auth.path("user").path("email").asText();
        JsonNode loggedIn = json(call("POST", "/api/auth/login", null,
                Map.of("email", email.toUpperCase(java.util.Locale.ROOT), "password", PASSWORD), 200));
        assertThat(loggedIn.path("accessToken").asText()).isNotBlank();
        call("POST", "/api/auth/login", null, Map.of("email", email, "password", "wrong"), 401);
        call("POST", "/api/auth/login", null, Map.of("email", email, "password", "x".repeat(100)), 401);
        call("POST", "/api/auth/register", null,
                Map.of("email", email.toUpperCase(java.util.Locale.ROOT), "fullName", "Otro", "password", PASSWORD), 409);
        call("POST", "/api/auth/register", null, Map.of("email", "invalid", "password", "short"), 400);
        call("POST", "/api/auth/register", null,
                Map.of("email", "long@example.test", "fullName", "Otro", "password", "é".repeat(40)), 400);
    }

    @Test
    void jwtRejectsMalformedExpiredForgedDeletedAndInactiveUsers() throws Exception {
        JsonNode auth = register();
        String token = auth.path("accessToken").asText();
        String email = auth.path("user").path("email").asText();
        for (String invalid : java.util.List.of("invalid", "a.b.c", "a.%%%.b", token + ".", token + "x",
                signedToken("HS256", Map.of("sub", email, "exp", 1)),
                signedToken("none", Map.of("sub", email, "exp", Instant.now().getEpochSecond() + 1000)),
                signedToken("HS256", Map.of("exp", Instant.now().getEpochSecond() + 1000)),
                jwtTokenUtil.generateToken("missing@example.test"))) {
            JsonNode error = json(call("GET", "/api/users/me", invalid, null, 401));
            assertThat(error.path("status").asInt()).isEqualTo(401);
            assertThat(error.path("timestamp").asText()).isNotBlank();
            assertThat(error.path("message").asText()).isEqualTo("No autenticado");
        }
        call("GET", "/api/users/me", null, null, 401);
        jdbc.update("update users set status = 'INACTIVE' where user_id = ?", auth.path("user").path("userId").asLong());
        call("GET", "/api/users/me", token, null, 401);
        call("POST", "/api/auth/login", null, Map.of("email", email, "password", PASSWORD), 401);
        JsonNode deleted = register();
        jdbc.update("delete from users where user_id = ?", deleted.path("user").path("userId").asLong());
        call("GET", "/api/users/me", deleted.path("accessToken").asText(), null, 401);
    }

    @Test
    void corsAcceptsConfiguredFrontendAndRejectsOtherOrigins() throws Exception {
        for (String origin : java.util.List.of("http://localhost:3000", "https://untrusted.example")) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/clients"))
                    .header("Origin", origin).header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "authorization,content-type")
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(origin.contains("localhost") ? 200 : 403);
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin").orElse(""))
                    .isEqualTo(origin.contains("localhost") ? origin : "");
        }
    }

    @Test
    void clienteCrudIsIsolatedByJwtOwnerAndUpdatesTimestamps() throws Exception {
        JsonNode auth = register();
        String owner = auth.path("accessToken").asText();
        JsonNode otherAuth = register();
        String other = otherAuth.path("accessToken").asText();
        Map<String, Object> request = Map.of("clientType", "PERSON", "fullNameOrCompany", "Cliente inicial",
                "owner", Map.of("userId", otherAuth.path("user").path("userId").asLong()));
        JsonNode created = json(call("POST", "/api/clients", owner, request, 201));
        long id = created.path("clientId").asLong();
        assertThat(created.has("owner")).isFalse();
        assertThat(jdbc.queryForObject("select owner_user_id from clients where client_id = ?", Long.class, id))
                .isEqualTo(auth.path("user").path("userId").asLong());
        assertThat(json(call("GET", "/api/clients", other, null, 200))).isEmpty();
        assertThat(json(call("GET", "/api/clients", owner, null, 200)).size()).isEqualTo(1);
        assertThat(json(call("GET", "/api/clients/" + id, owner, null, 200)).path("clientId").asLong()).isEqualTo(id);
        for (String method : java.util.List.of("GET", "PUT", "DELETE")) {
            call(method, "/api/clients/" + id, other, method.equals("PUT") ? request : null, 404);
        }
        call("POST", "/api/clients", owner, Map.of("clientType", "PERSON"), 400);
        call("POST", "/api/clients", owner, Map.of("clientType", "INVALID", "fullNameOrCompany", "X"), 400);
        call("GET", "/api/clients/not-a-number", owner, null, 400);
        call("GET", "/api/clients/" + id, null, null, 401);
        JsonNode updated = json(call("PUT", "/api/clients/" + id, owner,
                Map.of("clientType", "COMPANY", "fullNameOrCompany", "Empresa actualizada"), 200));
        assertThat(updated.path("createdAt")).isEqualTo(created.path("createdAt"));
        assertThat(updated.path("updatedAt")).isNotEqualTo(created.path("updatedAt"));
        assertThat(updated.path("updatedAt"))
                .isEqualTo(json(call("GET", "/api/clients/" + id, owner, null, 200)).path("updatedAt"));
        assertThat(call("DELETE", "/api/clients/" + id, owner, null, 204).body()).isEmpty();
        call("GET", "/api/clients/" + id, owner, null, 404);
    }

    @Test
    void expedienteCrudValidatesOwnershipDatesAndStateTransitions() throws Exception {
        String owner = register().path("accessToken").asText();
        String other = register().path("accessToken").asText();
        long client = createClient(owner);
        long otherClient = createClient(other);
        Map<String, Object> request = Map.of("clientId", client, "title", "Expediente inicial", "openedAt", "2026-01-01");
        JsonNode created = json(call("POST", "/api/cases", owner, request, 201));
        long id = created.path("caseId").asLong();
        assertThat(created.path("status").asText()).isEqualTo("OPEN");
        assertThat(json(call("GET", "/api/cases", other, null, 200))).isEmpty();
        assertThat(json(call("GET", "/api/cases", owner, null, 200)).size()).isEqualTo(1);
        for (String method : java.util.List.of("GET", "PUT", "DELETE")) {
            call(method, "/api/cases/" + id, other, method.equals("PUT") ? request : null, 404);
        }
        call("POST", "/api/cases", other, request, 404);
        call("PUT", "/api/cases/" + id, owner, Map.of("clientId", otherClient, "title", "Cambio ajeno"), 404);
        call("GET", "/api/clients/" + client + "/cases", other, null, 404);
        assertThat(json(call("GET", "/api/clients/" + client + "/cases", owner, null, 200)).size()).isEqualTo(1);
        call("POST", "/api/cases", owner, Map.of("title", "Falta cliente"), 400);
        call("POST", "/api/cases", owner, Map.of("clientId", client, "title", "Fechas invalidas",
                "status", "CLOSED", "openedAt", "2026-02-01", "closedAt", "2026-01-01"), 400);
        call("PUT", "/api/cases/" + id, owner, Map.of("clientId", client, "title", "Abierto con cierre",
                "status", "OPEN", "closedAt", "2026-02-01"), 400);
        JsonNode closed = json(call("PUT", "/api/cases/" + id, owner,
                Map.of("clientId", client, "title", "Cerrado", "status", "CLOSED"), 200));
        assertThat(closed.path("openedAt").asText()).isEqualTo("2026-01-01");
        assertThat(closed.path("closedAt").asText()).isNotBlank();
        assertThat(closed.path("updatedAt")).isNotEqualTo(created.path("updatedAt"));
        JsonNode edited = json(call("PUT", "/api/cases/" + id, owner,
                Map.of("clientId", client, "title", "Cerrado editado"), 200));
        assertThat(edited.path("status").asText()).isEqualTo("CLOSED");
        assertThat(edited.path("closedAt")).isEqualTo(closed.path("closedAt"));
        JsonNode reopened = json(call("PUT", "/api/cases/" + id, owner,
                Map.of("clientId", client, "title", "Reabierto", "status", "IN_PROGRESS"), 200));
        assertThat(reopened.path("closedAt").isNull()).isTrue();
        call("DELETE", "/api/clients/" + client, owner, null, 409);
        call("GET", "/api/clients/" + client, owner, null, 200);
        call("DELETE", "/api/cases/" + id, owner, null, 204);
        call("GET", "/api/cases/" + id, owner, null, 404);
        call("DELETE", "/api/clients/" + client, owner, null, 204);
    }

    @Test
    void documentoMetadataIsValidatedAndIsolatedByExpedienteOwner() throws Exception {
        JsonNode auth = register();
        String owner = auth.path("accessToken").asText();
        String other = register().path("accessToken").asText();
        long client = createClient(owner);
        long expediente = json(call("POST", "/api/cases", owner,
                Map.of("clientId", client, "title", "Caso con documentos"), 201)).path("caseId").asLong();
        String path = "/api/cases/" + expediente + "/documents";
        Map<String, Object> request = Map.of("fileName", "contrato.pdf", "fileType", "application/pdf",
                "storageUrl", "https://storage.example.test/contrato.pdf", "sizeBytes", 1024,
                "uploadedBy", 999999, "processingStatus", "ERROR");
        JsonNode created = json(call("POST", path, owner, request, 201));
        long id = created.path("documentId").asLong();
        assertThat(created.path("processingStatus").asText()).isEqualTo("UPLOADED");
        assertThat(jdbc.queryForObject("select uploaded_by_user_id from documents where document_id = ?", Long.class, id))
                .isEqualTo(auth.path("user").path("userId").asLong());
        call("GET", path, other, null, 404);
        call("POST", path, other, request, 404);
        call("GET", "/api/documents/" + id, other, null, 404);
        call("DELETE", "/api/documents/" + id, other, null, 404);
        call("POST", path, owner, Map.of("fileName", ""), 400);
        call("POST", path, owner, Map.of("fileName", "invalid.pdf", "sizeBytes", -1), 400);
        call("POST", "/api/cases/9223372036854775807/documents", owner, request, 404);
        assertThat(json(call("GET", path, owner, null, 200)).size()).isEqualTo(1);
        assertThat(json(call("GET", "/api/documents/" + id, owner, null, 200)).path("fileName").asText())
                .isEqualTo("contrato.pdf");
        call("DELETE", "/api/cases/" + expediente, owner, null, 409);
        call("GET", "/api/documents/" + id, owner, null, 200);
        assertThat(call("DELETE", "/api/documents/" + id, owner, null, 204).body()).isEmpty();
        call("GET", "/api/documents/" + id, owner, null, 404);
        assertThat(json(call("GET", path, owner, null, 200))).isEmpty();
        call("DELETE", "/api/cases/" + expediente, owner, null, 204);
    }

    @Test
    void auditRecordsSuccessfulActionsAndRollsBackFailedChanges() throws Exception {
        JsonNode auth = register();
        long userId = auth.path("user").path("userId").asLong();
        String owner = auth.path("accessToken").asText();
        call("POST", "/api/auth/login", null,
                Map.of("email", auth.path("user").path("email").asText(), "password", PASSWORD), 200);
        long client = createClient(owner);
        call("PUT", "/api/clients/" + client, owner,
                Map.of("clientType", "COMPANY", "fullNameOrCompany", "Empresa auditada"), 200);
        long expediente = json(call("POST", "/api/cases", owner,
                Map.of("clientId", client, "title", "Caso auditado"), 201)).path("caseId").asLong();
        call("DELETE", "/api/clients/" + client, owner, null, 409);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id = ? and action = 'DELETE_CLIENT'",
                Long.class, userId)).isZero();
        call("PUT", "/api/cases/" + expediente, owner,
                Map.of("clientId", client, "title", "Caso auditado", "status", "CLOSED"), 200);
        call("PUT", "/api/cases/" + expediente, owner,
                Map.of("clientId", client, "title", "Caso auditado editado", "status", "CLOSED"), 200);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id = ? and action = 'CLOSE_CASE'",
                Long.class, userId)).isEqualTo(1);
        long document = json(call("POST", "/api/cases/" + expediente + "/documents", owner,
                Map.of("fileName", "auditado.pdf"), 201)).path("documentId").asLong();
        call("DELETE", "/api/documents/" + document, owner, null, 204);
        call("DELETE", "/api/cases/" + expediente, owner, null, 204);
        call("DELETE", "/api/clients/" + client, owner, null, 204);
        assertThat(jdbc.queryForList("select action from audit_logs where user_id = ? order by log_id", String.class, userId))
                .containsExactly("LOGIN", "CREATE_CLIENT", "UPDATE_CLIENT", "CREATE_CASE", "UPDATE_CASE",
                        "CLOSE_CASE", "UPDATE_CASE", "UPLOAD_DOCUMENT", "DELETE_DOCUMENT", "DELETE_CLIENT");
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id = ? and "
                + "(created_at is null or entity_id is null or entity_type is null)", Long.class, userId)).isZero();
        assertThat(jdbc.queryForList("select details from audit_logs where user_id = ?", String.class, userId))
                .noneMatch(detail -> detail.contains(PASSWORD) || detail.contains(owner));
        long newClient = createClient(owner);
        call("POST", "/api/cases", owner, Map.of("clientId", newClient, "title", "Creado cerrado", "status", "CLOSED"), 201);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where user_id = ? and action = 'CLOSE_CASE'",
                Long.class, userId)).isEqualTo(2);
    }

    @Test
    void swaggerDocumentsEveryEndpointWithBearerAndErrorSchemas() throws Exception {
        call("GET", "/swagger-ui/index.html", null, null, 200);
        JsonNode docs = json(call("GET", "/v3/api-docs", null, null, 200));
        JsonNode schemes = docs.path("components").path("securitySchemes");
        assertThat(schemes.path("Bearer Token").path("scheme").asText()).isEqualTo("bearer");
        assertThat(docs.toString()).doesNotContain("passwordHash");
        JsonNode upload = docs.path("paths").path("/api/cases/{caseId}/documents/upload").path("post");
        JsonNode uploadSchema = upload.path("requestBody").path("content").path("multipart/form-data").path("schema");
        if (uploadSchema.has("$ref")) {
            uploadSchema = docs.at(uploadSchema.path("$ref").asText().substring(1));
        }
        assertThat(uploadSchema.path("properties").path("file").path("format").asText()).isEqualTo("binary");
        assertThat(uploadSchema.path("properties").path("category").path("type").asText()).isEqualTo("string");
        assertThat(upload.toString()).contains("category", "caseId");
        for (String status : java.util.List.of("201", "400", "401", "403", "404", "413", "500")) {
            assertThat(upload.path("responses").has(status)).as("upload " + status).isTrue();
            if (!status.equals("201")) {
                assertThat(upload.path("responses").path(status).path("content").path("application/json")
                        .path("schema").path("$ref").asText()).endsWith("ErrorResponse");
            }
        }
        Map<String, java.util.List<String>> endpoints = Map.ofEntries(
                Map.entry("/api/auth/register", java.util.List.of("post")),
                Map.entry("/api/auth/login", java.util.List.of("post")),
                Map.entry("/api/users/me", java.util.List.of("get")),
                Map.entry("/api/roles", java.util.List.of("get")),
                Map.entry("/api/clients", java.util.List.of("get", "post")),
                Map.entry("/api/clients/{id}", java.util.List.of("get", "put", "delete")),
                Map.entry("/api/clients/{clientId}/cases", java.util.List.of("get")),
                Map.entry("/api/cases", java.util.List.of("get", "post")),
                Map.entry("/api/cases/{id}", java.util.List.of("get", "put", "delete")),
                Map.entry("/api/cases/{caseId}/documents", java.util.List.of("get", "post")),
                Map.entry("/api/documents/{id}", java.util.List.of("get", "delete")));
        endpoints.forEach((path, methods) -> methods.forEach(method -> {
            JsonNode operation = docs.path("paths").path(path).path(method);
            assertThat(operation.path("summary").asText()).as(path + " " + method).isNotBlank();
            String success = method.equals("delete") ? "204"
                    : method.equals("post") && !path.endsWith("login") ? "201" : "200";
            assertThat(operation.path("responses").has(success)).as(path + " " + method).isTrue();
            if (!method.equals("delete")) {
                assertThat(operation.path("responses").path(success).path("content")
                        .path("*/*").path("schema").isMissingNode()
                        && operation.path("responses").path(success).path("content")
                        .path("application/json").path("schema").isMissingNode()).isFalse();
            }
            if (path.startsWith("/api/auth/")) {
                assertThat(operation.path("security")).isEmpty();
            } else {
                assertThat(operation.path("responses").path("401").path("content")
                        .path("application/json").path("schema").path("$ref").asText()).endsWith("ErrorResponse");
            }
            if (method.equals("post") || method.equals("put")) {
                assertThat(operation.path("requestBody").path("content").path("application/json")
                        .path("schema").path("$ref").asText()).contains("RequestDTO");
            }
        }));
    }

    @Test
    void concurrentRegistrationKeepsEmailUnique() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String body = objectMapper.writeValueAsString(Map.of("email", email, "fullName", "Registro concurrente",
                "password", PASSWORD));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/register"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var first = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> firstResponse = first.get();
        HttpResponse<String> secondResponse = second.get();
        Long userId = jdbc.queryForObject("select user_id from users where email = ?", Long.class, email);
        createdUsers.add(userId);
        assertThat(java.util.List.of(firstResponse.statusCode(), secondResponse.statusCode()))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Long.class, email)).isEqualTo(1);
    }

    @Test
    void realMultipartPersistsFileMetadataAuditAndEnforcesOwnershipAndLimit() throws Exception {
        JsonNode auth = register();
        String owner = auth.path("accessToken").asText();
        long userId = auth.path("user").path("userId").asLong();
        long caseId = json(call("POST", "/api/cases", owner,
                Map.of("clientId", createClient(owner), "title", "Carga PDF"), 201)).path("caseId").asLong();
        String pdf = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n";
        String foreignToken = register().path("accessToken").asText();
        upload(caseId, foreignToken, pdf, 404);
        upload(Long.MAX_VALUE, owner, pdf, 404);
        upload(caseId, null, pdf, 401);
        upload(caseId, owner, "%PDF-1.7\n" + "x".repeat(1024), 413);
        assertThat(jdbc.queryForObject("select count(*) from documents where case_id = ?", Long.class, caseId)).isZero();
        JsonNode result = json(upload(caseId, owner, pdf, 201));
        long documentId = result.path("documentId").asLong();
        assertThat(result.path("sizeBytes").asLong()).isEqualTo(pdf.getBytes(StandardCharsets.UTF_8).length);
        assertThat(result.path("processingStatus").asText()).isEqualTo("UPLOADED");
        assertThat(result.path("fileName").asText()).isEqualTo("contrato.pdf");
        assertThat(result.path("category").asText()).isEqualTo("Contrato");
        String storageId = result.path("storageUrl").asText();
        assertThat(storageId).matches("[a-f0-9-]{36}\\.pdf");
        assertThat(java.nio.file.Files.readString(uploadStorage.resolve(storageId))).isEqualTo(pdf);
        assertThat(jdbc.queryForObject("select uploaded_by_user_id from documents where document_id = ?", Long.class, documentId))
                .isEqualTo(userId);
        assertThat(jdbc.queryForObject("select details from audit_logs where action = 'UPLOAD_DOCUMENT' and entity_id = ? and user_id = ?",
                String.class, documentId, userId)).isEqualTo("caseId=" + caseId + "; fileName=contrato.pdf");
    }

    private HttpResponse<String> upload(long caseId, String token, String pdf, int expected) throws Exception {
        String boundary = "LegalAI" + UUID.randomUUID();
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"contrato.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n" + pdf + "\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"category\"\r\n\r\nContrato\r\n--" + boundary + "--\r\n";
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/cases/" + caseId + "/documents/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null) request.header("Authorization", "Bearer " + token);
        var response = http.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
        if (expected != 201) assertThat(json(response).path("status").asInt()).isEqualTo(expected);
        return response;
    }

    @Test
    void processesRealPdfAndProtectsTextAndPersistsErrors() throws Exception {
        JsonNode auth = register();
        String token = auth.path("accessToken").asText();
        long caseId = json(call("POST", "/api/cases", token,
                Map.of("clientId", createClient(token), "title", "Extraccion PDF"), 201)).path("caseId").asLong();
        java.nio.file.Path pdf = uploadStorage.resolve("processing-test.pdf");
        DocumentoProcessingTest.pdf(pdf, "Contrato de prueba LegalAI");
        long id = json(call("POST", "/api/cases/" + caseId + "/documents", token,
                Map.of("fileName", "test.pdf", "fileType", "application/pdf", "storageUrl", pdf.getFileName().toString(),
                        "category", "CONTRACT", "sizeBytes", java.nio.file.Files.size(pdf)), 201)).path("documentId").asLong();
        String endpoint = "/api/documents/" + id;
        String foreign = register().path("accessToken").asText();
        call("POST", endpoint + "/process", null, null, 401);
        call("GET", endpoint + "/text", null, null, 401);
        call("POST", endpoint + "/process", foreign, null, 404);
        call("GET", endpoint + "/text", foreign, null, 404);
        call("POST", "/api/documents/9223372036854775807/process", token, null, 404);
        JsonNode result = json(call("POST", endpoint + "/process", token, null, 200));
        assertThat(result.path("processingStatus").asText()).isEqualTo("PROCESSED");
        assertThat(result.path("hasExtractedText").asBoolean()).isTrue();
        assertThat(result.path("extractedTextLength").asInt()).isEqualTo(26);
        assertThat(result.has("extractedText")).isFalse();
        assertThat(json(call("GET", endpoint + "/text", token, null, 200)).path("text").asText()).isEqualTo("Contrato de prueba LegalAI");
        assertThat(jdbc.queryForObject("select extracted_text from documents where document_id=?", String.class, id)).isEqualTo("Contrato de prueba LegalAI");
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where entity_id=? and action='PROCESS_DOCUMENT'", Long.class, id)).isEqualTo(1);
        assertThat(json(call("POST", endpoint + "/chunks", token, null, 200)).path("chunksCreated").asInt()).isEqualTo(1);
        jdbc.execute("alter table audit_logs add constraint test_processing_audit_failure check (action <> 'PROCESS_DOCUMENT') not valid");
        try {
            call("POST", endpoint + "/process", token, null, 500);
            assertThat(jdbc.queryForObject("select processing_status from documents where document_id=?", String.class, id)).isEqualTo("ERROR");
            assertThat(jdbc.queryForObject("select extracted_text from documents where document_id=?", String.class, id)).isNull();
            assertThat(jdbc.queryForObject("select count(*) from document_chunks where document_id=?", Long.class, id)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where entity_id=? and action='PROCESS_DOCUMENT'", Long.class, id)).isEqualTo(1);
        } finally {
            jdbc.execute("alter table audit_logs drop constraint test_processing_audit_failure");
        }
        call("POST", endpoint + "/process", token, null, 200);
        java.nio.file.Files.delete(pdf);
        call("POST", endpoint + "/process", token, null, 500);
        assertThat(jdbc.queryForObject("select processing_status from documents where document_id=?", String.class, id)).isEqualTo("ERROR");
        assertThat(jdbc.queryForObject("select extracted_text from documents where document_id=?", String.class, id)).isNull();
        JsonNode docs = json(call("GET", "/v3/api-docs", null, null, 200));
        assertThat(docs.path("paths").has("/api/documents/{documentId}/process")).isTrue();
        assertThat(docs.path("paths").has("/api/documents/{documentId}/text")).isTrue();
    }

    private long createClient(String token) throws Exception {
        return json(call("POST", "/api/clients", token,
                Map.of("clientType", "PERSON", "fullNameOrCompany", "Cliente de prueba"), 201)).path("clientId").asLong();
    }

    @Test
    void chunksAreOwnedOrderedAtomicAndRegeneratedWithoutDuplicates() throws Exception {
        JsonNode auth = register();
        String token = auth.path("accessToken").asText();
        long caseId = json(call("POST", "/api/cases", token,
                Map.of("clientId", createClient(token), "title", "Chunks test"), 201)).path("caseId").asLong();
        long id = json(call("POST", "/api/cases/" + caseId + "/documents", token,
                Map.of("fileName", "chunks.pdf", "fileType", "application/pdf", "category", "CONTRACT"), 201)).path("documentId").asLong();
        String endpoint = "/api/documents/" + id + "/chunks";
        String foreign = register().path("accessToken").asText();
        call("POST", endpoint, null, null, 401);
        call("GET", endpoint, null, null, 401);
        call("POST", endpoint, foreign, null, 404);
        call("GET", endpoint, foreign, null, 404);
        call("POST", "/api/documents/9223372036854775807/chunks", token, null, 404);
        call("GET", "/api/documents/9223372036854775807/chunks", token, null, 404);
        call("POST", endpoint, token, null, 400);
        assertThat(json(call("GET", endpoint, token, null, 200))).isEmpty();
        jdbc.update("update documents set processing_status='PROCESSED',extracted_text=? where document_id=?", "Texto corto", id);
        assertThat(json(call("POST", endpoint, token, null, 200)).path("chunksCreated").asInt()).isEqualTo(1);
        String text = "Articulo 1. El contrato establece obligaciones para las partes.\r\n\r\n".repeat(180);
        String normalized = pe.edu.upc.legalai.servicesimplements.CharacterChunker.normalize(text);
        jdbc.update("update documents set extracted_text=? where document_id=?", text, id);
        int count = json(call("POST", endpoint, token, null, 200)).path("chunksCreated").asInt();
        assertThat(count).isGreaterThan(1);
        var result = json(call("GET", endpoint, token, null, 200));
        assertThat(result.size()).isEqualTo(count);
        for (int i = 0; i < count; i++) {
            JsonNode chunk = result.get(i);
            int start = chunk.path("charStart").asInt(), end = chunk.path("charEnd").asInt();
            assertThat(chunk.path("chunkIndex").asInt()).isEqualTo(i);
            assertThat(chunk.path("documentId").asLong()).isEqualTo(id);
            assertThat(start).isLessThan(end);
            assertThat(chunk.path("content").asText()).isNotBlank().isEqualTo(normalized.substring(start,end).trim());
            if (i > 0) assertThat(result.get(i-1).path("charEnd").asInt() - start).isEqualTo(300);
        }
        call("POST", endpoint, token, null, 200);
        assertThat(jdbc.queryForObject("select count(*) from document_chunks where document_id=?", Long.class, id)).isEqualTo(count);
        assertThat(jdbc.queryForObject("select processing_status from documents where document_id=?", String.class, id)).isEqualTo("PROCESSED");
        var before = jdbc.queryForList("select chunk_id from document_chunks where document_id=? order by chunk_index", Long.class, id);
        Long audits = jdbc.queryForObject("select count(*) from audit_logs where entity_id=? and action='GENERATE_DOCUMENT_CHUNKS'", Long.class,id);
        jdbc.execute("alter table document_chunks add constraint test_chunk_insert_failure check (chunk_index < 0) not valid");
        try {
            call("POST", endpoint, token, null, 500);
            assertThat(jdbc.queryForList("select chunk_id from document_chunks where document_id=? order by chunk_index", Long.class,id)).isEqualTo(before);
        } finally { jdbc.execute("alter table document_chunks drop constraint test_chunk_insert_failure"); }
        jdbc.execute("alter table audit_logs add constraint test_chunk_audit_failure check (action <> 'GENERATE_DOCUMENT_CHUNKS') not valid");
        try {
            call("POST", endpoint, token, null, 500);
            assertThat(jdbc.queryForList("select chunk_id from document_chunks where document_id=? order by chunk_index", Long.class,id)).isEqualTo(before);
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where entity_id=? and action='GENERATE_DOCUMENT_CHUNKS'",Long.class,id)).isEqualTo(audits);
        } finally { jdbc.execute("alter table audit_logs drop constraint test_chunk_audit_failure"); }
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+endpoint))
                .header("Authorization", "Bearer "+token).POST(HttpRequest.BodyPublishers.noBody()).build();
        var first = http.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        assertThat(first.get().statusCode()).isEqualTo(200); assertThat(second.get().statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(*) from document_chunks where document_id=?",Long.class,id)).isEqualTo(count);
        JsonNode docs = json(call("GET", "/v3/api-docs",null,null,200));
        assertThat(docs.path("paths").path("/api/documents/{documentId}/chunks").has("post")).isTrue();
        assertThat(docs.path("paths").path("/api/documents/{documentId}/chunks").has("get")).isTrue();
        call("DELETE", "/api/documents/"+id, token,null,204);
        assertThat(jdbc.queryForObject("select count(*) from document_chunks where document_id=?",Long.class,id)).isZero();
    }

    private String signedToken(String algorithm, Map<String, Object> claims) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String data = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", algorithm, "typ", "JWT")))
                + "." + encoder.encodeToString(objectMapper.writeValueAsBytes(claims));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-only-secret-not-for-deployment-legalai-2026".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return data + "." + encoder.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private JsonNode register() throws Exception {
        JsonNode auth = json(call("POST", "/api/auth/register", null, Map.of(
                "fullName", "Usuario de prueba", "email", UUID.randomUUID() + "@example.test",
                "password", PASSWORD), 201));
        createdUsers.add(auth.path("user").path("userId").asLong());
        return auth;
    }

    private HttpResponse<String> call(String method, String path, String token, Object body, int status)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(status);
        return response;
    }

    private JsonNode json(HttpResponse<String> response) {
        return objectMapper.readTree(response.body());
    }

}
