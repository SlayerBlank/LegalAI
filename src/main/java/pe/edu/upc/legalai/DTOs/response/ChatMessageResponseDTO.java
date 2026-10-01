package pe.edu.upc.legalai.DTOs.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Turno completo: pregunta persistida y respuesta generada con las fuentes consultadas")
public record ChatMessageResponseDTO(
        @Schema(example = "12") Long sessionId,
        ChatMessageDTO userMessage,
        ChatMessageDTO assistantMessage,
        @Schema(description = "Proveedor que respondio") String provider,
        @Schema(description = "Modelo real que respondio, incluido el fallback") String model,
        @Schema(description = "Numero de fragmentos enviados al modelo; null si es una respuesta historica sin metadatos")
        Integer retrievedChunks,
        @Schema(description = "CONSULTED_FRAGMENTS: fragmentos consultados, sin verificacion de respaldo por afirmacion")
        String sourcesType,
        @Schema(description = "Fragmentos reales enviados al modelo; null si no se conservaron metadatos. No son citas verificadas por afirmacion")
        List<RAGSourceDTO> sources) { }