package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.legalai.dtos.request.ChatHistoryTurnDTO;
import pe.edu.upc.legalai.dtos.response.IAResponseDTO;
import pe.edu.upc.legalai.configs.ChatSettings;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.servicesimplements.ChatQueryRewriter;
import pe.edu.upc.legalai.servicesinterfaces.IAService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatQueryRewriterTest {

    final IAService ia = mock(IAService.class);
    final List<ChatHistoryTurnDTO> historial = List.of(
            new ChatHistoryTurnDTO("USER", "¿Cual es la potencia optica de la Falcon A1 Pro?"),
            new ChatHistoryTurnDTO("ASSISTANT", "La potencia optica es 20 W [F1]"));

    ChatQueryRewriter rewriter(String mode) {
        return new ChatQueryRewriter(ia, new ChatSettings(10, 2000, 20, 100, mode));
    }

    @Test void independentQuestionIsUsedDirectlyAndCostsNoExtraCall() {
        assertThat(rewriter("heuristic").retrievalQuery("¿Cual es el area de trabajo?", historial))
                .isEqualTo("¿Cual es el area de trabajo?");
        assertThat(rewriter("llm").retrievalQuery("¿Cual es el area de trabajo?", historial))
                .isEqualTo("¿Cual es el area de trabajo?");
        verifyNoInteractions(ia);
    }

    @Test void emptyHistoryKeepsTheOriginalQuestion() {
        assertThat(rewriter("heuristic").retrievalQuery("¿Y cual es su peso?", List.of()))
                .isEqualTo("¿Y cual es su peso?");
        verifyNoInteractions(ia);
    }

    @Test void heuristicModeCombinesTheLastUserQuestionWithoutGemini() {
        assertThat(rewriter("heuristic").retrievalQuery("¿Y cual es su peso?", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? ¿Y cual es su peso?");
        assertThat(rewriter("heuristic").retrievalQuery("Y el area de trabajo?", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? Y el area de trabajo?");
        assertThat(rewriter("heuristic").retrievalQuery("Menciona las obligaciones anteriores", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? Menciona las obligaciones anteriores");
        verifyNoInteractions(ia);
    }

    @Test void llmModeRewritesOnlyDependentQuestionsAndFallsBackSafely() {
        IAResponseDTO answer = new IAResponseDTO();
        answer.setAnswer("¿Que sucede si no se cumple el plazo de pago establecido en el contrato?");
        org.mockito.Mockito.doReturn(answer).when(ia).generarRespuesta(any());
        assertThat(rewriter("llm").retrievalQuery("¿Y que pasa si no se cumple?", historial))
                .isEqualTo("¿Que sucede si no se cumple el plazo de pago establecido en el contrato?");

        org.mockito.Mockito.doThrow(new IAServiceException()).when(ia).generarRespuesta(any());
        assertThat(rewriter("llm").retrievalQuery("¿Y que pasa si no se cumple?", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? ¿Y que pasa si no se cumple?");

        IAResponseDTO multiLine = new IAResponseDTO();
        multiLine.setAnswer("linea uno\nlinea dos");
        org.mockito.Mockito.doReturn(multiLine).when(ia).generarRespuesta(any());
        assertThat(rewriter("llm").retrievalQuery("¿Y que pasa si no se cumple?", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? ¿Y que pasa si no se cumple?");

        IAResponseDTO tooLong = new IAResponseDTO();
        tooLong.setAnswer("x".repeat(500));
        org.mockito.Mockito.doReturn(tooLong).when(ia).generarRespuesta(any());
        assertThat(rewriter("llm").retrievalQuery("¿Y que pasa si no se cumple?", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? ¿Y que pasa si no se cumple?");

        IAResponseDTO vacio = new IAResponseDTO();
        vacio.setAnswer("   ");
        org.mockito.Mockito.doReturn(vacio).when(ia).generarRespuesta(any());
        assertThat(rewriter("llm").retrievalQuery("¿Y que pasa si no se cumple?", historial))
                .isEqualTo("¿Cual es la potencia optica de la Falcon A1 Pro? ¿Y que pasa si no se cumple?");
    }

    @Test void llmPromptCarriesOnlyAuthorizedHistoryAndTheCurrentQuestion() {
        IAResponseDTO answer = new IAResponseDTO();
        answer.setAnswer("Consulta autonoma");
        when(ia.generarRespuesta(any())).thenReturn(answer);
        var captor = org.mockito.ArgumentCaptor.forClass(pe.edu.upc.legalai.dtos.request.IARequestDTO.class);
        rewriter("llm").retrievalQuery("¿Y que pasa?", historial);
        verify(ia).generarRespuesta(captor.capture());
        assertThat(captor.getValue().getPrompt())
                .contains("HISTORIAL AUTORIZADO", "Usuario: ¿Cual es la potencia optica",
                        "Asistente: La potencia optica es 20 W", "Pregunta actual: ¿Y que pasa?")
                .doesNotContain("role", "ASSISTANT\n");
    }

    @Test void llmPromptIsBoundedByTheTurnWindow() {
        IAResponseDTO answer = new IAResponseDTO();
        answer.setAnswer("Consulta autonoma");
        when(ia.generarRespuesta(any())).thenReturn(answer);
        List<ChatHistoryTurnDTO> largo = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            largo.add(new ChatHistoryTurnDTO("USER", "pregunta-" + i + " " + "y".repeat(600)));
        }
        var captor = org.mockito.ArgumentCaptor.forClass(pe.edu.upc.legalai.dtos.request.IARequestDTO.class);
        rewriter("llm").retrievalQuery("¿Y que pasa?", largo);
        verify(ia).generarRespuesta(captor.capture());
        assertThat(captor.getValue().getPrompt()).contains("pregunta-39").doesNotContain("pregunta-0 ");
        assertThat(captor.getValue().getPrompt().length()).isLessThan(6000);
    }

    @Test void modeNoneDisablesReformulation() {
        assertThat(rewriter("none").retrievalQuery("¿Y cual es su peso?", historial))
                .isEqualTo("¿Y cual es su peso?");
        verifyNoInteractions(ia);
    }

    @Test void historyWithoutUserTurnKeepsTheOriginalQuestion() {
        assertThat(rewriter("heuristic").retrievalQuery("¿Y cual es su peso?",
                        List.of(new ChatHistoryTurnDTO("ASSISTANT", "sin pregunta previa"))))
                .isEqualTo("¿Y cual es su peso?");
    }

    @Test void heuristicFallbackTruncatesTheAnchorToKeepTheQueryBounded() {
        String previous = "p".repeat(1900);
        assertThat(rewriter("heuristic").retrievalQuery("¿Y cual es su peso?",
                        List.of(new ChatHistoryTurnDTO("USER", previous))))
                .isEqualTo("p".repeat(300) + " ¿Y cual es su peso?");
        String longQuestion = "q".repeat(1800);
        assertThat(rewriter("heuristic").retrievalQuery(longQuestion,
                        List.of(new ChatHistoryTurnDTO("USER", "ancla anterior"))))
                .isEqualTo(longQuestion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "51", "-1"})
    void invalidHistoryWindowIsRejected(int size) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new ChatSettings(size, 2000, 20, 100, "heuristic"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "otro", "1"})
    void unsupportedRewriteModesAreRejectedAtStartup(String mode) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new ChatSettings(10, 2000, 20, 100, mode))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "heuristic", "llm", " HEURISTIC "})
    void supportedRewriteModesAreNormalized(String mode) {
        assertThat(new ChatSettings(10, 2000, 20, 100, mode).queryRewrite()).isEqualTo(mode.trim().toLowerCase());
    }

    @Test void llmModeIsNeverCalledWithoutRealUserTurns() {
        IAResponseDTO answer = new IAResponseDTO();
        answer.setAnswer("Consulta autonoma");
        when(ia.generarRespuesta(any())).thenReturn(answer);
        assertThat(rewriter("llm").retrievalQuery("¿Y cual es su peso?",
                List.of(new ChatHistoryTurnDTO("ASSISTANT", "solo respuesta")))).isEqualTo("¿Y cual es su peso?");
        verify(ia, never()).generarRespuesta(any());
    }
}