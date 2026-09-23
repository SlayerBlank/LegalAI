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

    private long createClient(String token) throws Exception {
        return json(call("POST", "/api/clients", token,
                Map.of("clientType", "PERSON", "fullNameOrCompany", "Cliente de prueba"), 201)).path("clientId").asLong();
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
