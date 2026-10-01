package pe.edu.upc.legalai.dtos.response;

import java.util.List;

public record ChatTurnMetadata(
        String provider,
        String model,
        Integer retrievedChunks,
        String sourcesType,
        List<RAGSourceDTO> sources) { }
