package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.upc.legalai.servicesimplements.CharacterChunker;
import static org.assertj.core.api.Assertions.*;

class CharacterChunkerTest {
    @Test void shortDocumentAndNormalization() {
        var chunks = new CharacterChunker(2500, 300).split("  Articulo   1.\r\n\r\n\r\n\r\nTexto legal.  ");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().content()).isEqualTo("Articulo 1.\n\n\nTexto legal.");
        assertThat(chunks.getFirst().charStart()).isZero();
        assertThat(chunks.getFirst().charEnd()).isEqualTo(chunks.getFirst().content().length());
    }

    @Test void exactSlidingOverlapAndOffsets() {
        String text = "abcdefghij".repeat(700);
        var chunks = new CharacterChunker(2500, 300).split(text);
        assertThat(chunks).hasSize(4);
        for (int i = 0; i < chunks.size(); i++) {
            var chunk = chunks.get(i);
            assertThat(chunk.charStart()).isEqualTo(i * 2200);
            assertThat(chunk.charEnd()).isEqualTo(Math.min(i * 2200 + 2500, text.length()));
            assertThat(chunk.content()).isEqualTo(text.substring(chunk.charStart(), chunk.charEnd()));
            if (i > 0) assertThat(chunks.get(i-1).charEnd() - chunk.charStart()).isEqualTo(300);
        }
    }

    @Test void prefersParagraphWithoutTinyChunks() {
        String text = "a".repeat(60) + "\n\n" + "b".repeat(200);
        var chunks = new CharacterChunker(100, 20).split(text);
        assertThat(chunks.getFirst().charEnd()).isEqualTo(62);
        assertThat(chunks.get(1).charStart()).isEqualTo(42);
        assertThat(new CharacterChunker(100, 20).split("tiny\n\n" + "a".repeat(200)).getFirst().charEnd()).isEqualTo(100);
    }

    @ParameterizedTest @CsvSource({"1,0", "2,1", "100,99", "100,0", "100,30", "2500,300"})
    void windowsAdvanceCoverTextAndNeverReturnEmpty(int size, int overlap) {
        String source = ("Articulo 1.   Obligaciones.\r\n\r\n" + "x".repeat(150) + "\n").repeat(30);
        String normalized = CharacterChunker.normalize(source);
        var chunks = new CharacterChunker(size, overlap).split(source);
        int lastStart = -1, lastEnd = 0;
        for (var chunk : chunks) {
            assertThat(chunk.charStart()).isGreaterThan(lastStart);
            assertThat(chunk.charEnd()).isGreaterThan(lastEnd);
            assertThat(chunk.charEnd() - chunk.charStart()).isBetween(1, size);
            assertThat(chunk.content()).isNotBlank().isEqualTo(normalized.substring(chunk.charStart(), chunk.charEnd()).trim());
            // Any gap is whitespace only (empty windows are not emitted).
            if (chunk.charStart() > lastEnd) assertThat(normalized.substring(lastEnd, chunk.charStart())).isBlank();
            lastStart = chunk.charStart(); lastEnd = chunk.charEnd();
        }
        assertThat(lastEnd).isEqualTo(normalized.length());
    }

    @ParameterizedTest @CsvSource({"0,0", "-1,0", "10,-1", "10,10", "10,11"})
    void rejectsInvalidConfig(int size, int overlap) {
        assertThatThrownBy(() -> new CharacterChunker(size, overlap)).isInstanceOf(IllegalArgumentException.class);
    }
}
