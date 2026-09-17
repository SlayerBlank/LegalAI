package pe.edu.upc.legalai.dtos.client;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.legalai.entities.ClientType;

import java.time.LocalDateTime;

@Schema(description = "Datos de un cliente")
public class ClientResponseDTO {

    @Schema(description = "Identificador del cliente", example = "1")
    private Long clientId;

    @Schema(description = "Identificador del usuario propietario", example = "1")
    private Long ownerUserId;

    @Schema(description = "Tipo de cliente", example = "PERSON")
    private ClientType clientType;

    @Schema(description = "Nombre de la persona o razón social", example = "Carlos López")
    private String fullNameOrCompany;

    @Schema(description = "Número de documento", example = "12345678")
    private String documentNumber;

    @Schema(description = "Correo electrónico", example = "carlos@email.com")
    private String email;

    @Schema(description = "Teléfono", example = "999999999")
    private String phone;

    @Schema(description = "Dirección", example = "Lima")
    private String address;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    public ClientResponseDTO() {
    }

    public ClientResponseDTO(Long clientId, Long ownerUserId, ClientType clientType, String fullNameOrCompany,
                             String documentNumber, String email, String phone, String address,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.clientId = clientId;
        this.ownerUserId = ownerUserId;
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

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getFullNameOrCompany() {
        return fullNameOrCompany;
    }

    public void setFullNameOrCompany(String fullNameOrCompany) {
        this.fullNameOrCompany = fullNameOrCompany;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
