package pe.edu.upc.legalai.exceptions;

public class ChunkGenerationException extends RuntimeException {
    public ChunkGenerationException(Throwable cause) { super("No se pudieron generar los chunks del documento", cause); }
}
