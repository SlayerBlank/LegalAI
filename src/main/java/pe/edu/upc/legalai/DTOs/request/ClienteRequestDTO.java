package pe.edu.upc.legalai.DTOs.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.legalai.entities.TipoCliente;

@Schema(description = "Datos para crear o actualizar un cliente")
public class ClienteRequestDTO {

    @NotNull(message = "El tipo de cliente es obligatorio")
    private TipoCliente clientType;

    @NotBlank(message = "El nombre o razon social es obligatorio")
    @Size(max = 200, message = "El nombre o razon social no debe superar los 200 caracteres")
    private String fullNameOrCompany;

    @Size(max = 50, message = "El numero de documento no debe superar los 50 caracteres")
    private String documentNumber;

    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 150, message = "El correo no debe superar los 150 caracteres")
    private String email;

    @Size(max = 30, message = "El telefono no debe superar los 30 caracteres")
    private String phone;

    @Size(max = 255, message = "La direccion no debe superar los 255 caracteres")
    private String address;

    public TipoCliente getClientType() {
        return clientType;
    }

    public void setClientType(TipoCliente clientType) {
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
