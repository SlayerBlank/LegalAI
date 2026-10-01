package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.*;
import pe.edu.upc.legalai.config.EmbeddingSettings;
import pe.edu.upc.legalai.exceptions.EmbeddingException;
import pe.edu.upc.legalai.servicesimplements.GeminiEmbeddingServiceImpl;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class GeminiEmbeddingServiceTest {
    final EmbeddingSettings settings = new EmbeddingSettings("gemini-embedding-001", 768, 2);
    GeminiEmbeddingServiceImpl service(ExchangeFunction exchange) {
        return new GeminiEmbeddingServiceImpl(WebClient.builder().baseUrl("https://gemini.invalid/v1beta")
                .exchangeFunction(exchange).build(), "test-key", Duration.ofMillis(200), settings);
    }
    Mono<ClientResponse> response(int status, String body) {
        return Mono.just(ClientResponse.create(HttpStatusCode.valueOf(status))
                .header("Content-Type", "application/json").body(body).build());
    }
    String vector() { return "{\"values\":[3,4" + ",0".repeat(766) + "]}"; }

    @Test void verifiesEndpointTaskDimensionAndNormalizes() {
        for (String task : List.of("RETRIEVAL_DOCUMENT", "RETRIEVAL_QUERY")) {
            var provider = service(outgoing -> {
                assertThat(outgoing.url().toString()).endsWith("/models/gemini-embedding-001:embedContent");
                assertThat(outgoing.headers().getFirst("x-goog-api-key")).isEqualTo("test-key");
                var request = new org.springframework.mock.http.client.reactive.MockClientHttpRequest(outgoing.method(), outgoing.url());
                outgoing.writeTo(request, ExchangeStrategies.withDefaults()).block();
                var payload = tools.jackson.databind.json.JsonMapper.builder().build().readTree(request.getBodyAsString().block());
                assertThat(payload.path("taskType").asText()).isEqualTo(task);
                assertThat(payload.path("outputDimensionality").asInt()).isEqualTo(768);
                assertThat(payload.path("content").path("parts").get(0).path("text").asText()).isEqualTo("texto");
                return response(200, "{\"embedding\":" + vector() + "}");
            });
            float[] result = task.equals("RETRIEVAL_QUERY") ? provider.generarEmbeddingConsulta("texto") : provider.generarEmbedding("texto");
            assertThat(result).hasSize(768);
            assertThat(result[0]).isEqualTo(0.6f);
            assertThat(result[1]).isEqualTo(0.8f);
        }
    }

    @Test void batchKeepsOrderAndValidatesCount() {
        var provider = service(r -> {
            assertThat(r.url().toString()).endsWith(":batchEmbedContents");
            return response(200, "{\"embeddings\":[" + vector() + "," + vector() + "]}");
        });
        assertThat(provider.generarEmbeddings(List.of("a", "b"))).hasSize(2);
        assertThatThrownBy(() -> service(r -> response(200, "{\"embeddings\":[]}"))
                .generarEmbeddings(List.of("a", "b"))).isInstanceOf(EmbeddingException.class);
    }

    @ParameterizedTest @ValueSource(ints = {400, 401, 429, 500, 503})
    void sanitizedHttpErrors(int status) {
        assertThatThrownBy(() -> service(r -> response(status,"private-document test-key"))
                .generarEmbedding("private-document")).isInstanceOf(EmbeddingException.class)
                .hasNoCause().hasMessageNotContaining("private-document").hasMessageNotContaining("test-key");
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"embedding\":{\"values\":[]}}", "{\"embedding\":{\"values\":[1,2]}}", "not-json"})
    void rejectsMalformedResponses(String body) {
        assertThatThrownBy(() -> service(r -> response(200,body)).generarEmbedding("texto"))
                .isInstanceOf(EmbeddingException.class).hasNoCause();
    }

    @Test void timeoutAndInvalidVectors() {
        assertThatThrownBy(() -> service(r -> Mono.never()).generarEmbedding("texto"))
                .isInstanceOf(EmbeddingException.class).hasMessageContaining("timeout").hasNoCause();
        assertThatThrownBy(() -> settings.validate(new float[768])).isInstanceOf(EmbeddingException.class);
        var values = new float[768]; values[0] = Float.NaN;
        assertThatThrownBy(() -> settings.validate(values)).isInstanceOf(EmbeddingException.class);
        values[0] = Float.POSITIVE_INFINITY;
        assertThatThrownBy(() -> settings.validate(values)).isInstanceOf(EmbeddingException.class);
        assertThatThrownBy(() -> new EmbeddingSettings("generative-model",768,1)).isInstanceOf(IllegalArgumentException.class);
    }
}
