package pe.edu.upc.legalai.servicesimplements;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class CharacterChunker {
    private final int chunkSize;
    private final int overlap;

    public CharacterChunker(@Value("${legalai.rag.chunk-size:2500}") int chunkSize,
                            @Value("${legalai.rag.chunk-overlap:300}") int overlap) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk config invalida: chunkSize > 0 y 0 <= overlap < chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public int getChunkSize() { return chunkSize; }
    public int getOverlap() { return overlap; }

    public static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]{2,}", " ")
                .replaceAll("\\n{4,}", "\n\n\n").strip();
    }

    // Offsets are UTF-16 indices into normalized text: [charStart, charEnd).
    // Trimming affects content only, so content == normalized.substring(start, end).trim().
    public List<Fragment> split(String source) {
        String text = normalize(source);
        List<Fragment> result = new ArrayList<>();
        int start = 0;
        int previousEnd = 0;
        while (start < text.length()) {
            int end = start + Math.min(chunkSize, text.length() - start);
            if (end < text.length()) {
                int minimumEnd = Math.max(start + Math.max(chunkSize / 2, overlap + 1), previousEnd + 1);
                end = boundary(text, start, end, minimumEnd);
            }
            String content = text.substring(start, end).trim();
            if (!content.isBlank()) result.add(new Fragment(start, end, content));
            if (end == text.length()) break;
            previousEnd = end;
            start = Math.max(start + 1, end - overlap);
        }
        return result;
    }

    private int boundary(String text, int start, int end, int minimumEnd) {
        for (String separator : new String[]{"\n\n", "\n", ". ", " "}) {
            int at = text.lastIndexOf(separator, end - separator.length());
            int candidate = at + separator.length();
            if (at >= start && candidate >= minimumEnd) return candidate;
        }
        return end;
    }

    public record Fragment(int charStart, int charEnd, String content) { }
}
