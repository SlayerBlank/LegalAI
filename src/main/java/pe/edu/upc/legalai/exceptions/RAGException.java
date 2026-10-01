package pe.edu.upc.legalai.exceptions;

public class RAGException extends RuntimeException {
    public RAGException() { super("No se pudo completar la consulta documental. Reintente mas tarde."); }
}
