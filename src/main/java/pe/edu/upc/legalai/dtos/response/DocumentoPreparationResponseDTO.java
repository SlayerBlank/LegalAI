package pe.edu.upc.legalai.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;

@Schema(description = "Estado calculado con las columnas existentes; PROCESSED solo acredita extraccion. "
        + "readyForRag exige texto, fragmentos y embeddings validos para el modelo configurado. "
        + "No representa un trabajo en segundo plano ni almacena un estado nuevo en BD.")
public record DocumentoPreparationResponseDTO(Long documentId, EstadoProcesamiento processingStatus,
        String stage, boolean readyForRag, long chunks, long embeddedChunks) { }
