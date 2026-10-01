package pe.edu.upc.legalai.dtos.request;

import java.util.List;

/** Internal provider request, never bound to an HTTP endpoint. */
public record IAContextRequestDTO(String systemInstruction, String context, String question,
                                  List<ChatHistoryTurnDTO> historial) {

    public IAContextRequestDTO(String systemInstruction, String context, String question) {
        this(systemInstruction, context, question, List.of());
    }
}