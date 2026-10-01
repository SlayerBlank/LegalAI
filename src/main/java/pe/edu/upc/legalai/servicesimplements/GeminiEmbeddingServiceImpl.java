package pe.edu.upc.legalai.servicesimplements;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.upc.legalai.config.EmbeddingSettings;
import pe.edu.upc.legalai.exceptions.EmbeddingException;
import pe.edu.upc.legalai.servicesinterfaces.EmbeddingService;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.*;

@Service
public class GeminiEmbeddingServiceImpl implements EmbeddingService {
    private final WebClient client;
    private final String apiKey;
    private final Duration timeout;
    private final EmbeddingSettings settings;

    public GeminiEmbeddingServiceImpl(@Qualifier("geminiWebClient") WebClient client,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.request-timeout}") Duration timeout, EmbeddingSettings settings) {
        this.client = client;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.settings = settings;
    }

    @Override public float[] generarEmbedding(String texto) {
        return single(texto, "RETRIEVAL_DOCUMENT");
    }

    @Override public float[] generarEmbeddingConsulta(String texto) {
        return single(texto, "RETRIEVAL_QUERY");
    }

    private float[] single(String text, String task) {
        return vector(call("embedContent", payload(text, task)).path("embedding"));
    }

    @Override public List<float[]> generarEmbeddings(List<String> texts) {
        if (texts.isEmpty()) return List.of();
        if (texts.size() > settings.batchSize()) throw new EmbeddingException("Lote de embeddings demasiado grande");
        if (texts.size() == 1) return List.of(generarEmbedding(texts.getFirst()));
        var requests = texts.stream().map(t -> payload(t, "RETRIEVAL_DOCUMENT")).toList();
        JsonNode embeddings = call("batchEmbedContents", Map.of("requests", requests)).path("embeddings");
        if (!embeddings.isArray() || embeddings.size() != texts.size())
            throw new EmbeddingException("Gemini devolvio un lote de embeddings incompleto");
        List<float[]> result = new ArrayList<>();
        for (JsonNode embedding : embeddings) result.add(vector(embedding));
        return result;
    }

    private Map<String, Object> payload(String text, String task) {
        if (text == null || text.isBlank()) throw new EmbeddingException("Texto de embedding vacio");
        return Map.of("model", "models/" + settings.model(),
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "taskType", task, "outputDimensionality", settings.dimension());
    }

    // One in-flight request per application instance, including concurrent HTTP callers.
    // No automatic retries: a 429 aborts the document and can be retried by the caller later.
    private synchronized JsonNode call(String operation, Object payload) {
        if (apiKey == null || apiKey.isBlank()) throw new EmbeddingException("Gemini embeddings no configurado");
        try {
            JsonNode response = client.post().uri("/models/{model}:{operation}", settings.model(), operation)
                    .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON).bodyValue(payload)
                    .exchangeToMono(result -> {
                        int status = result.statusCode().value();
                        if (!result.statusCode().is2xxSuccessful()) {
                            String message = status == 429 ? "Limite de Gemini embeddings alcanzado (429); reintente mas tarde"
                                    : status >= 500 ? "Gemini embeddings temporalmente no disponible (5xx)"
                                    : "Gemini rechazo la solicitud de embeddings";
                            return result.releaseBody().then(Mono.<JsonNode>error(new EmbeddingException(message)));
                        }
                        return result.bodyToMono(JsonNode.class);
                    }).block(timeout);
            if (response == null) throw new EmbeddingException("Respuesta de embeddings vacia");
            return response;
        } catch (EmbeddingException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Never retain upstream causes: they may expose credentials or document text.
            throw new EmbeddingException("Fallo o timeout al consultar Gemini embeddings");
        }
    }

    private float[] vector(JsonNode embedding) {
        JsonNode values = embedding.path("values");
        if (!values.isArray() || values.size() != settings.dimension())
            throw new EmbeddingException("Gemini devolvio una dimension de embedding invalida");
        float[] vector = new float[values.size()];
        double norm = 0;
        for (int i = 0; i < vector.length; i++) {
            if (!values.get(i).isNumber()) throw new EmbeddingException("Embedding no numerico");
            vector[i] = (float) values.get(i).asDouble();
            norm += (double) vector[i] * vector[i];
        }
        settings.validate(vector);
        double length = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / length);
        settings.validate(vector);
        return vector;
    }
}
