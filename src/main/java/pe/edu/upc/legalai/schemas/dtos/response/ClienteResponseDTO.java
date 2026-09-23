package pe.edu.upc.legalai.schemas.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.TipoCliente;

import java.time.LocalDateTime;

@Schema(description = "Datos de un cliente")
public class ClienteResponseDTO {

    private Long clientId;
    private TipoCliente clientType;
    private String fullNameOrCompany;
    private String documentNumber;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ClienteResponseDTO(Long clientId, TipoCliente clientType, String fullNameOrCompany, String documentNumber,
                              String email, String phone, String address, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.clientId = clientId;
        this.clientType = clientType;
        this.fullNameOrCompany = fullNameOrCompany;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getClientId() {
        return clientId;
    }

    public TipoCliente getClientType() {
        return clientType;
    }

    public String getFullNameOrCompany() {
        return fullNameOrCompany;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
