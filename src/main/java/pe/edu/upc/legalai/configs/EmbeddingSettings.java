package pe.edu.upc.legalai.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.upc.legalai.exceptions.EmbeddingException;
import java.util.Arrays;

@Component
public class EmbeddingSettings {
    private final String model;
    private final int dimension;
    private final int batchSize;

    public EmbeddingSettings(@Value("${gemini.embedding.model}") String model,
            @Value("${legalai.embedding.dimension}") int dimension,
            @Value("${legalai.embedding.batch-size}") int batchSize) {
        // Other models need their own verified task/payload contract, not just a name change.
        if (!"gemini-embedding-001".equals(model))
            throw new IllegalArgumentException("Modelo de embeddings no soportado: use gemini-embedding-001");
        if (dimension < 128 || dimension > 3072)
            throw new IllegalArgumentException("La dimension de gemini-embedding-001 debe estar entre 128 y 3072");
        if (batchSize < 1 || batchSize > 16)
            throw new IllegalArgumentException("embedding.batch-size debe estar entre 1 y 16");
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
    }

    public String model() { return model; }
    public int dimension() { return dimension; }
    public int batchSize() { return batchSize; }

    public void validate(float[] vector) {
        if (vector == null || vector.length != dimension)
            throw new EmbeddingException("Dimension de embedding invalida; esperada: " + dimension);
        double norm = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) throw new EmbeddingException("Embedding con valores no finitos");
            norm += (double) value * value;
        }
        if (norm == 0) throw new EmbeddingException("Embedding nulo: no permite distancia coseno");
    }

    public String vectorLiteral(float[] vector) {
        validate(vector);
        return Arrays.toString(vector);
    }
}
