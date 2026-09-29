package pe.edu.upc.legalai.servicesimplements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.upc.legalai.DTOs.request.IARequestDTO;
import pe.edu.upc.legalai.DTOs.response.IAResponseDTO;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.servicesinterfaces.IAService;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GeminiIAServiceImpl implements IAService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiIAServiceImpl.class);
    private static final String SYSTEM_INSTRUCTION = """
            Eres un asistente de apoyo para profesionales del derecho.
            Proporciona información clara, estructurada y prudente.
            No presentes las respuestas como asesoramiento jurídico definitivo.
            No inventes hechos, documentos, nombres, fechas o cláusulas.
            Si la información disponible no es suficiente, indícalo claramente.
            El profesional del derecho es responsable de revisar y validar la información generada.
            """;

    private final WebClient client;
    private final String apiKey;
    private final String model;
    private final String fallbackModel;
    private final Duration timeout;

    public GeminiIAServiceImpl(@Qualifier("geminiWebClient") WebClient client,
                               @Value("${gemini.api.key}") String apiKey,
                               @Value("${gemini.model}") String model,
                               @Value("${gemini.fallback-model}") String fallbackModel,
                               @Value("${gemini.request-timeout}") Duration timeout) {
        this.client = client;
        this.apiKey = apiKey;
        this.model = model;
        this.fallbackModel = fallbackModel;
        this.timeout = timeout;
    }

    @Override
    public IAResponseDTO generarRespuesta(IARequestDTO request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IAServiceException();
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", request.getPrompt())))));
        try {
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    return callModel(body, model, attempt, false);
                } catch (TransientHttpException ex) {
                    if (attempt == 3) {
                        return callModel(body, fallbackModel, 1, true);
                    }
                    // Tres intentos totales: pausas de 1s y 2s, sin espera tras el ultimo.
                    Thread.sleep(1000L << (attempt - 1));
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IAServiceException();
        } catch (RuntimeException ex) {
            // No conservar causas externas: pueden contener cuerpos o credenciales.
            throw new IAServiceException();
        }
        throw new IAServiceException();
    }

    private IAResponseDTO callModel(Map<String, Object> body, String selectedModel, int attempt, boolean fallback) {
        long startedAt = System.nanoTime();
        AtomicInteger status = new AtomicInteger();
        try {
            JsonNode response = client.post().uri("/models/{model}:generateContent", selectedModel)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchangeToMono(result -> {
                        int httpStatus = result.statusCode().value();
                        status.set(httpStatus);
                        if (!result.statusCode().is2xxSuccessful()) {
                            RuntimeException error = httpStatus == 429 || httpStatus == 503
                                    ? new TransientHttpException() : new IAServiceException();
                            return result.releaseBody().then(Mono.<JsonNode>error(error));
                        }
                        return result.bodyToMono(JsonNode.class);
                    })
                    .block(timeout);
            IAResponseDTO dto = new IAResponseDTO();
            dto.setAnswer(extractAnswer(response));
            dto.setProvider("gemini");
            dto.setModel(selectedModel);
            return dto;
        } finally {
            // Solo metadatos operativos, nunca cuerpos, prompts, headers ni excepciones.
            LOGGER.info("Gemini request: model={} attempt={} httpStatus={} durationMs={} fallback={}",
                    safeModel(selectedModel), attempt, status.get() == 0 ? "unavailable" : status.get(),
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis(), fallback);
        }
    }

    private String safeModel(String selectedModel) {
        String sanitized = selectedModel.replace(apiKey, "[REDACTED]");
        return sanitized.matches("[A-Za-z0-9._-]{1,120}") ? sanitized : "[INVALID_MODEL]";
    }

    private static final class TransientHttpException extends RuntimeException {
    }
    private String extractAnswer(JsonNode response) {
        if (response == null || response.path("promptFeedback").has("blockReason")) {
            throw new IAServiceException();
        }
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IAServiceException();
        }
        JsonNode candidate = candidates.get(0);
        String finishReason = candidate.path("finishReason").asText();
        if (!"STOP".equals(finishReason)) {
            throw new IAServiceException();
        }
        JsonNode parts = candidate.path("content").path("parts");
        if (!parts.isArray()) {
            throw new IAServiceException();
        }
        StringBuilder answer = new StringBuilder();
        for (JsonNode part : parts) {
            if (!part.path("thought").asBoolean(false) && part.path("text").isString()) {
                answer.append(part.path("text").asText());
            }
        }
        if (answer.toString().isBlank()) {
            throw new IAServiceException();
        }
        return answer.toString().trim();
    }
}
