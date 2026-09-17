package pe.edu.upc.legalai.dtos.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.entities.ClientType;

@Schema(description = "Datos requeridos para crear o actualizar un cliente")
public class ClientRequestDTO {

    @Schema(description = "Identificador del usuario propietario del cliente", example = "1")
    @NotNull(message = "El usuario propietario es obligatorio")
    private Long ownerUserId;

    @Schema(description = "Tipo de cliente", example = "PERSON")
    @NotNull(message = "El tipo de cliente es obligatorio")
    private ClientType clientType;

    @Schema(description = "Nombre de la persona o razón social de la empresa", example = "Carlos López")
    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 200, message = "El nombre o razón social no debe superar los 200 caracteres")
    private String fullNameOrCompany;

    @Schema(description = "Número de documento de identidad o RUC", example = "12345678")
    @Size(max = 50, message = "El número de documento no debe superar los 50 caracteres")
    private String documentNumber;

    @Schema(description = "Correo electrónico del cliente", example = "carlos@email.com")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 150, message = "El correo electrónico no debe superar los 150 caracteres")
    private String email;

    @Schema(description = "Teléfono de contacto", example = "999999999")
    @Size(max = 30, message = "El teléfono no debe superar los 30 caracteres")
    private String phone;

    @Schema(description = "Dirección del cliente", example = "Lima")
    @Size(max = 255, message = "La dirección no debe superar los 255 caracteres")
    private String address;

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
}
