package pe.edu.upc.legalai.servicesinterfaces;

import java.util.List;

public interface EmbeddingService {
    float[] generarEmbedding(String texto);
    float[] generarEmbeddingConsulta(String texto);
    default List<float[]> generarEmbeddings(List<String> textos) {
        return textos.stream().map(this::generarEmbedding).toList();
    }
}
