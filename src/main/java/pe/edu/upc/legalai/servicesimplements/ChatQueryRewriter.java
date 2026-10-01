package pe.edu.upc.legalai.servicesimplements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.edu.upc.legalai.DTOs.request.ChatHistoryTurnDTO;
import pe.edu.upc.legalai.DTOs.request.IARequestDTO;
import pe.edu.upc.legalai.config.ChatSettings;
import pe.edu.upc.legalai.config.RAGSettings;
import pe.edu.upc.legalai.servicesinterfaces.IAService;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Convierte una pregunta conversacional en una consulta de recuperacion autonomous.
 * La pregunta original nunca se modifica: solo la consulta enviada a pgvector cambia.
 */
@Component
public class ChatQueryRewriter {

    private static final Logger LOG = LoggerFactory.getLogger(ChatQueryRewriter.class);
    private static final int MAX_TURNS_IN_PROMPT = 6;
    private static final int MAX_TURN_CHARS = 500;
    private static final int MAX_REWRITTEN_CHARS = 400;
    private static final int MAX_ANCHOR_CHARS = 300;
    private static final String USER = "USER";

    private static final List<String> ANAPHORA = List.of(
            "y cual", "y que", "y cuanto", "y cuando", "y donde", "y quienes", "y entonces", "y por",
            "y a su vez", "y sobre",
            "especifica", "especifico", "especificamente", "detallame", "amplia",
            "anterior", "anteriores", "antes mencionad", "mencionad", "citad", "referid",
            "dicho", "dicha", "dichos", "dichas", "este", "esta", "esto", "ese", "esa", "eso",
            "ellos", "ellas", "su ", "sus ", "lo anterior", "lo mismo", "lo indicado",
            "el mismo", "la misma", "en cuanto a", "sobre eso", "en ese caso", "de lo anterior");

    private static final List<String> INICIO_SEGUIDO = List.of(
            " y ", " o ", " y entonces ", " entonces ", " y si ", " bueno ", " ahora ");

    private static final String REWRITE_INSTRUCTION = """
            Reescribe la ultima pregunta del usuario como una consulta autonomous y verificable.
            Debe conservar el idioma de la pregunta y resolver las referencias al HISTORIAL
            (pronombres, elipsis y "y ...") usando solo la conversacion proporcionada.
            Devuelve SOLO la consulta reformulada, sin comillas, sin explicaciones y sin inventar
            datos que no aparecen en el HISTORIAL. Si la pregunta ya es autonoma, devulvela tal cual.
            """;

    private final IAService ia;
    private final ChatSettings settings;

    public ChatQueryRewriter(IAService ia, ChatSettings settings) {
        this.ia = ia;
        this.settings = settings;
    }

    public String retrievalQuery(String question, List<ChatHistoryTurnDTO> HISTORIAL) {
        if (settings.queryRewrite().equals(ChatSettings.QUERY_REWRITE_NONE)
                || !dependeDelHISTORIAL(question)) {
            return question;
        }
        String ancla = ultimaPreguntaDelUsuario(HISTORIAL);
        if (ancla == null) {
            return question;
        }
        if (settings.queryRewrite().equals(ChatSettings.QUERY_REWRITE_LLM)) {
            String rewritten = rewriterConGemini(HISTORIAL, question);
            if (rewritten != null) {
                return rewritten;
            }
        }
        return heuristica(ancla, question);
    }

    public boolean dependeDelHISTORIAL(String question) {
        String normalized = normalize(question);
        if (INICIO_SEGUIDO.stream().anyMatch(normalized::startsWith)) {
            return true;
        }
        return ANAPHORA.stream().anyMatch(normalized::contains);
    }

    private String heuristica(String ancla, String question) {
        String previous = ancla.length() > MAX_ANCHOR_CHARS ? ancla.substring(0, MAX_ANCHOR_CHARS) : ancla;
        String combined = previous + " " + question;
        return combined.length() <= RAGSettings.MAX_QUESTION_CHARS ? combined : question;
    }

    private String ultimaPreguntaDelUsuario(List<ChatHistoryTurnDTO> HISTORIAL) {
        if (HISTORIAL == null) {
            return null;
        }
        return HISTORIAL.stream()
                .filter(Objects::nonNull)
                .filter(turn -> USER.equals(turn.role()))
                .map(ChatHistoryTurnDTO::content)
                .filter(content -> content != null && !content.isBlank())
                .map(String::trim)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private String rewriterConGemini(List<ChatHistoryTurnDTO> HISTORIAL, String question) {
        StringBuilder prompt = new StringBuilder(REWRITE_INSTRUCTION).append("\nHISTORIAL AUTORIZADO:\n");
        int used = 0;
        for (int index = Math.max(0, HISTORIAL.size() - MAX_TURNS_IN_PROMPT); index < HISTORIAL.size(); index++) {
            ChatHistoryTurnDTO turn = HISTORIAL.get(index);
            if (turn == null || turn.content() == null || turn.content().isBlank()) {
                continue;
            }
            used++;
            prompt.append(USER.equals(turn.role()) ? "Usuario: " : "Asistente: ")
                    .append(turn.content().length() > MAX_TURN_CHARS
                            ? turn.content().substring(0, MAX_TURN_CHARS) : turn.content())
                    .append('\n');
        }
        if (used == 0) {
            return null;
        }
        prompt.append("Pregunta actual: ").append(question);
        IARequestDTO request = new IARequestDTO();
        request.setPrompt(prompt.toString());
        try {
            var answer = ia.generarRespuesta(request);
            String rewritten = answer == null || answer.getAnswer() == null ? null : answer.getAnswer().trim();
            if (rewritten == null || rewritten.isBlank() || rewritten.length() > MAX_REWRITTEN_CHARS
                    || rewritten.contains("\n")) {
                return null;
            }
            return rewritten;
        } catch (RuntimeException ex) {
            LOG.warn("Reformulacion LLM no disponible; se usa la heuristica. type={}",
                    ex.getClass().getSimpleName());
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String plain = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", " ").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ");
        return " " + plain + " ";
    }
}