package pe.edu.upc.legalai.DTOs.response;

import java.util.List;

public record ChatTurnMetadata(
        String provider,
        String model,
        Integer retrievedChunks,
        String sourcesType,
        List<RAGSourceDTO> sources) { }
