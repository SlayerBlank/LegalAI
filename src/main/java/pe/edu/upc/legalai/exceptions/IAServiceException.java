package pe.edu.upc.legalai.exceptions;

public class IAServiceException extends RuntimeException {
    public IAServiceException() {
        super("El servicio de inteligencia artificial no está disponible.");
    }
}
