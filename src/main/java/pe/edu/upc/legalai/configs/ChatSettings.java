package pe.edu.upc.legalai.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ChatSettings {

    public static final String QUERY_REWRITE_NONE = "none";
    public static final String QUERY_REWRITE_HEURISTIC = "heuristic";
    public static final String QUERY_REWRITE_LLM = "llm";

    private final int maxHistoryMessages;
    private final int maxMessageChars;
    private final int maxPageSize;
    private final int defaultPageSize;
    private final String queryRewrite;

    public ChatSettings(@Value("${legalai.chat.max-history-messages:10}") int maxHistoryMessages,
            @Value("${legalai.chat.max-message-chars:2000}") int maxMessageChars,
            @Value("${legalai.chat.default-page-size:20}") int defaultPageSize,
            @Value("${legalai.chat.max-page-size:100}") int maxPageSize,
            @Value("${legalai.chat.query-rewrite:heuristic}") String queryRewrite) {
        if (maxHistoryMessages < 1 || maxHistoryMessages > 50)
            throw new IllegalArgumentException("legalai.chat.max-history-messages debe estar entre 1 y 50");
        if (maxMessageChars < 1 || maxMessageChars > RAGSettings.MAX_QUESTION_CHARS)
            throw new IllegalArgumentException("legalai.chat.max-message-chars debe estar entre 1 y "
                    + RAGSettings.MAX_QUESTION_CHARS);
        if (defaultPageSize < 1 || maxPageSize < 1 || defaultPageSize > maxPageSize || maxPageSize > 200)
            throw new IllegalArgumentException("legalai.chat page-size debe estar entre 1 y 200 y el default no puede superar el maximo");
        String mode = queryRewrite == null ? "" : queryRewrite.trim().toLowerCase(Locale.ROOT);
        if (!QUERY_REWRITE_NONE.equals(mode) && !QUERY_REWRITE_HEURISTIC.equals(mode)
                && !QUERY_REWRITE_LLM.equals(mode))
            throw new IllegalArgumentException("legalai.chat.query-rewrite debe ser none, heuristic o llm");
        this.maxHistoryMessages = maxHistoryMessages;
        this.maxMessageChars = maxMessageChars;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.queryRewrite = mode;
    }

    public int maxHistoryMessages() {
        return maxHistoryMessages;
    }

    public int maxMessageChars() {
        return maxMessageChars;
    }

    public int defaultPageSize() {
        return defaultPageSize;
    }

    public int maxPageSize() {
        return maxPageSize;
    }

    public String queryRewrite() {
        return queryRewrite;
    }
}