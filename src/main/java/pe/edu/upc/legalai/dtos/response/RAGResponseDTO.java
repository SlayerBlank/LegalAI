package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RAGResponseDTO(String answer, String provider, String model,
        @Schema(description = "Numero de fragmentos seleccionados y enviados al modelo") int retrievedChunks,
        @Schema(description = "CONSULTED_FRAGMENTS: fuentes consultadas, sin verificacion de respaldo por afirmacion")
        String sourcesType, List<RAGSourceDTO> sources) { }
