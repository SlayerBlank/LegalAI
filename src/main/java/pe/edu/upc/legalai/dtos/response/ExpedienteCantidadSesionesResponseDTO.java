package pe.edu.upc.legalai.dtos.response;

public class ExpedienteCantidadSesionesResponseDTO {

    private Long caseId;
    private String tituloExpediente;
    private Long cantidadSesiones;

    public ExpedienteCantidadSesionesResponseDTO(Long caseId, String tituloExpediente, Long cantidadSesiones) {
        this.caseId = caseId;
        this.tituloExpediente = tituloExpediente;
        this.cantidadSesiones = cantidadSesiones;
    }

    public Long getCaseId() { return caseId; }
    public String getTituloExpediente() { return tituloExpediente; }
    public Long getCantidadSesiones() { return cantidadSesiones; }
}