package pe.edu.upc.legalai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RAGSettings {
    public static final int MAX_QUESTION_CHARS = 2000;
    private final int defaultTopK;
    private final int maxContextChars;
    private final Double maxCosineDistance;

    public RAGSettings(@Value("${legalai.rag.default-top-k:5}") int defaultTopK,
            @Value("${legalai.rag.max-context-chars:12000}") int maxContextChars,
            @Value("${legalai.rag.max-cosine-distance:}") String maxCosineDistance) {
        if (defaultTopK < 1 || defaultTopK > 10) throw new IllegalArgumentException("RAG default-top-k debe estar entre 1 y 10");
        if (maxContextChars < 256 || maxContextChars > 100000)
            throw new IllegalArgumentException("RAG max-context-chars debe estar entre 256 y 100000");
        this.defaultTopK = defaultTopK;
        this.maxContextChars = maxContextChars;
        this.maxCosineDistance = maxCosineDistance == null || maxCosineDistance.isBlank()
                ? null : Double.valueOf(maxCosineDistance.trim());
        if (this.maxCosineDistance != null && (!Double.isFinite(this.maxCosineDistance)
                || this.maxCosineDistance < 0 || this.maxCosineDistance > 2))
            throw new IllegalArgumentException("RAG max-cosine-distance debe estar vacio o entre 0 y 2");
    }
    public int defaultTopK() { return defaultTopK; }
    public int maxContextChars() { return maxContextChars; }
    public Double maxCosineDistance() { return maxCosineDistance; }
}
