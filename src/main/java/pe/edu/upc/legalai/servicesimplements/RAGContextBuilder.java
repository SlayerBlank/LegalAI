package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Component;
import pe.edu.upc.legalai.dtos.response.*;
import pe.edu.upc.legalai.configs.RAGSettings;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Component
public class RAGContextBuilder {
    private final RAGSettings settings;
    private final ObjectMapper mapper;
    public RAGContextBuilder(RAGSettings settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    public record Context(String text, List<RAGSourceDTO> sources) { }

    public Context build(List<SemanticSearchResultDTO> candidates, int topK) {
        List<RAGSourceDTO> selected = new ArrayList<>();
        String context = "[]";
        for (var chunk : candidates) {
            if (selected.size() >= topK) break;
            if (!Double.isFinite(chunk.distance()) || chunk.content() == null || chunk.content().isBlank()) continue;
            if (settings.maxCosineDistance() != null && chunk.distance() > settings.maxCosineDistance()) continue;
            if (selected.stream().anyMatch(previous -> redundant(previous, chunk))) continue;
            var source = new RAGSourceDTO("F" + (selected.size() + 1), chunk.documentId(), chunk.documentName(),
                    chunk.chunkId(), chunk.chunkIndex(), chunk.content(), chunk.distance(), chunk.charStart(), chunk.charEnd());
            var proposed = new ArrayList<>(selected);
            proposed.add(source);
            // JSON escaping keeps forged fragment headers/file names inside data values.
            String text = mapper.writeValueAsString(proposed);
            // Keep complete legal fragments. Skip oversized chunks; never silently truncate them.
            if (text.length() > settings.maxContextChars()) continue;
            selected.add(source);
            context = text;
        }
        return new Context(context, List.copyOf(selected));
    }

    private boolean redundant(RAGSourceDTO previous, SemanticSearchResultDTO chunk) {
        if (!previous.documentId().equals(chunk.documentId())) return false;
        if (previous.chunkId().equals(chunk.chunkId()) || previous.excerpt().contains(chunk.content())) return true;
        if (previous.charStart() == null || previous.charEnd() == null || chunk.charStart() == null || chunk.charEnd() == null)
            return false;
        long overlap = Math.max(0L, (long)Math.min(previous.charEnd(), chunk.charEnd())
                - Math.max(previous.charStart(), chunk.charStart()));
        long length = (long)chunk.charEnd() - chunk.charStart();
        // Avoid excessive overlap while retaining the ordinary ~12% chunk overlap.
        return length > 0 && overlap * 2 >= length;
    }
}
