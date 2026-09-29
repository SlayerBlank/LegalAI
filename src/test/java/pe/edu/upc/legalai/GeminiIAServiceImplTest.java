package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.upc.legalai.DTOs.request.IARequestDTO;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.servicesimplements.GeminiIAServiceImpl;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class GeminiIAServiceImplTest {
    private final IARequestDTO request = request();

    private static IARequestDTO request() {
        IARequestDTO dto = new IARequestDTO();
        dto.setPrompt("Explica una obligacion contractual");
        return dto;
    }

    private GeminiIAServiceImpl service(ExchangeFunction exchange, String key) {
        return new GeminiIAServiceImpl(WebClient.builder().baseUrl("https://gemini.invalid/v1beta")
                .exchangeFunction(exchange).build(), key, "configured-model", "fallback-model", Duration.ofMillis(100));
    }

    private Mono<ClientResponse> response(int status, String body) {
        return Mono.just(ClientResponse.create(HttpStatusCode.valueOf(status))
                .header("Content-Type", "application/json").body(body).build());
    }

    @Test
    void logsProviderDiagnosticWithoutCredentialsAndKeepsGenericException() {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(GeminiIAServiceImpl.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            String body = """
                    {"error":{"code":429,"status":"RESOURCE_EXHAUSTED",
                    "message":"Quota exceeded for test-key. Bearer secret-token eyJhbGciOiJIUzI1NiJ9.payload.signature\\nAuthorization: secret-header"}}
                    """;
            assertThatThrownBy(() -> service(r -> response(429, body), "test-key").generarRespuesta(request))
                    .isInstanceOf(IAServiceException.class)
                    .hasMessage("El servicio de inteligencia artificial no está disponible.").hasNoCause();
            assertThat(appender.list).hasSize(4);
            String log = appender.list.getFirst().getFormattedMessage();
            assertThat(log).contains("httpStatus=429", "attempt=1", "fallback=false",
                            "model=configured-model", "durationMs=")
                    .doesNotContain("test-key", "secret-token", "secret-header", "eyJhbGciOiJIUzI1NiJ9", "\n");
            assertThat(appender.list.getFirst().getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void malformedErrorBodyIsNotLogged() {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(GeminiIAServiceImpl.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> service(r -> response(503, "private raw body test-key"), "test-key")
                    .generarRespuesta(request)).isInstanceOf(IAServiceException.class);
            assertThat(appender.list).hasSize(4);
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .contains("httpStatus=503", "attempt=1")
                    .doesNotContain("private raw body", "test-key");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void extractsOnlyAnswerAndUsesConfiguredModelAndHeader() {
        var service = service(outgoing -> {
            assertThat(outgoing.url().toString()).isEqualTo("https://gemini.invalid/v1beta/models/configured-model:generateContent");
            assertThat(outgoing.headers().getFirst("x-goog-api-key")).isEqualTo("test-key");
            assertThat(outgoing.headers().getFirst("Authorization")).isNull();
            var httpRequest = new org.springframework.mock.http.client.reactive.MockClientHttpRequest(
                    outgoing.method(), outgoing.url());
            outgoing.writeTo(httpRequest, org.springframework.web.reactive.function.client.ExchangeStrategies.withDefaults())
                    .block(Duration.ofSeconds(1));
            var payload = tools.jackson.databind.json.JsonMapper.builder().build()
                    .readTree(httpRequest.getBodyAsString().block(Duration.ofSeconds(1)));
            assertThat(payload.path("systemInstruction").path("parts").get(0).path("text").asText())
                    .contains("profesionales del derecho").doesNotContain(request.getPrompt());
            assertThat(payload.path("contents").size()).isEqualTo(1);
            assertThat(payload.path("contents").get(0).path("parts").get(0).path("text").asText())
                    .isEqualTo(request.getPrompt());
            return response(200, """
                    {"candidates":[{"finishReason":"STOP","content":{"parts":[
                    {"text":"private reasoning","thought":true},{"text":"Una obligación "},{"text":"contractual."}]}}],
                    "usageMetadata":{"private":"metadata"}}
                    """);
        }, "test-key");
        var result = service.generarRespuesta(request);
        assertThat(result.getAnswer()).isEqualTo("Una obligación contractual.");
        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getModel()).isEqualTo("configured-model");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 500, 502})
    void sanitizesExternalErrorsWithoutRetries(int status) {
        AtomicInteger calls = new AtomicInteger();
        var service = service(outgoing -> {
            calls.incrementAndGet();
            return response(status, "private-provider-details");
        }, "test-key");
        assertThatThrownBy(() -> service.generarRespuesta(request)).isInstanceOf(IAServiceException.class)
                .hasMessage("El servicio de inteligencia artificial no está disponible.").hasNoCause();
        assertThat(calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json", "{\"candidates\":[]}",
            "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\" \"}]}}]}",
            "{\"candidates\":[{\"finishReason\":\"SAFETY\",\"content\":{\"parts\":[{\"text\":\"blocked\"}]}}]}",
            "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"partial\"}]}}]}"})
    void rejectsEmptyMalformedAndBlockedResponses(String body) {
        assertThatThrownBy(() -> service(r -> response(200, body), "test-key").generarRespuesta(request))
                .isInstanceOf(IAServiceException.class).hasNoCause();
    }

    @Test
    void timesOutWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();
        var service = service(r -> { calls.incrementAndGet(); return Mono.never(); }, "test-key");
        assertThatThrownBy(() -> service.generarRespuesta(request)).isInstanceOf(IAServiceException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void retriesTransientFailureThenReturnsPrimary(int status) {
        AtomicInteger calls = new AtomicInteger();
        long start = System.nanoTime();
        var result = service(r -> calls.incrementAndGet() == 1
                ? response(status, "private") : success(), "test-key").generarRespuesta(request);
        assertThat(calls.get()).isEqualTo(2);
        assertThat(result.getModel()).isEqualTo("configured-model");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void exhaustsPrimaryThenCallsFallbackOnceWithSameBody(int status) {
        var paths = new java.util.ArrayList<String>();
        var bodies = new java.util.ArrayList<String>();
        long start = System.nanoTime();
        var result = service(r -> {
            paths.add(r.url().getPath());
            var httpRequest = new org.springframework.mock.http.client.reactive.MockClientHttpRequest(r.method(), r.url());
            r.writeTo(httpRequest, org.springframework.web.reactive.function.client.ExchangeStrategies.withDefaults())
                    .block(Duration.ofSeconds(1));
            bodies.add(httpRequest.getBodyAsString().block(Duration.ofSeconds(1)));
            return paths.size() <= 3 ? response(status, "private") : success();
        }, "test-key").generarRespuesta(request);
        assertThat(paths).containsExactly("/v1beta/models/configured-model:generateContent",
                "/v1beta/models/configured-model:generateContent", "/v1beta/models/configured-model:generateContent",
                "/v1beta/models/fallback-model:generateContent");
        assertThat(bodies).hasSize(4).allMatch(b -> b.equals(bodies.getFirst()));
        assertThat(result.getModel()).isEqualTo("fallback-model");
        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getAnswer()).isEqualTo("Respuesta");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofSeconds(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429, 503})
    void failureOfBothModelsIsBoundedAndSanitized(int fallbackStatus) {
        AtomicInteger calls = new AtomicInteger();
        var service = service(r -> response(calls.incrementAndGet() <= 3 ? 503 : fallbackStatus, "private"), "test-key");
        assertThatThrownBy(() -> service.generarRespuesta(request)).isInstanceOf(IAServiceException.class).hasNoCause();
        assertThat(calls.get()).isEqualTo(4);
    }

    @Test
    void nonTransientErrorAfterRetryStopsWithoutFallback() {
        AtomicInteger calls = new AtomicInteger();
        var service = service(r -> response(calls.incrementAndGet() == 1 ? 503 : 400, "private"), "test-key");
        assertThatThrownBy(() -> service.generarRespuesta(request)).isInstanceOf(IAServiceException.class);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void interruptionStopsRetriesAndPreservesInterruptFlag() {
        AtomicInteger calls = new AtomicInteger();
        var service = service(r -> { calls.incrementAndGet(); Thread.currentThread().interrupt();
            return response(503, ""); }, "test-key");
        try {
            assertThatThrownBy(() -> service.generarRespuesta(request)).isInstanceOf(IAServiceException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            Thread.interrupted();
        }
    }

    private Mono<ClientResponse> success() {
        return response(200, """
                {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Respuesta"}]}}]}
                """);
    }
    @Test
    void missingKeyDoesNotCallProvider() {
        var service = service(r -> { throw new AssertionError("Unexpected external call"); }, " ");
        assertThatThrownBy(() -> service.generarRespuesta(request)).isInstanceOf(IAServiceException.class);
    }
}
