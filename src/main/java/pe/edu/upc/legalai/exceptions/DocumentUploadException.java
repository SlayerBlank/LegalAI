package pe.edu.upc.legalai.exceptions;

public class DocumentUploadException extends RuntimeException {
    public DocumentUploadException(Throwable cause) {
        super("No se pudo guardar el documento", cause);
    }
}
