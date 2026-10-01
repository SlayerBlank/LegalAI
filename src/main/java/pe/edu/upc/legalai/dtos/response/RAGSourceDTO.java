package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fragmento real enviado al modelo; no es una cita verificada por afirmacion. Sin numeros de pagina.")
public record RAGSourceDTO(String reference, Long documentId, String documentName, Long chunkId,
        int chunkIndex, String excerpt, double distance, Integer charStart, Integer charEnd) { }
