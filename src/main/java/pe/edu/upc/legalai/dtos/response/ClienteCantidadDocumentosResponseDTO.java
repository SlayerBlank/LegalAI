package pe.edu.upc.legalai.dtos.response;

public class ClienteCantidadDocumentosResponseDTO {

    private Long clientId;
    private String nombreCliente;
    private Long cantidadDocumentos;

    public ClienteCantidadDocumentosResponseDTO(Long clientId, String nombreCliente, Long cantidadDocumentos) {
        this.clientId = clientId;
        this.nombreCliente = nombreCliente;
        this.cantidadDocumentos = cantidadDocumentos;
    }

    public Long getClientId() { return clientId; }
    public String getNombreCliente() { return nombreCliente; }
    public Long getCantidadDocumentos() { return cantidadDocumentos; }
}