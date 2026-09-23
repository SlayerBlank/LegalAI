package pe.edu.upc.legalai.DTOs.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de autenticacion")
public class AuthResponseDTO {

    private String tokenType;
    private String accessToken;
    private UsuarioResponseDTO user;

    public AuthResponseDTO(String tokenType, String accessToken, UsuarioResponseDTO user) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public UsuarioResponseDTO getUser() {
        return user;
    }
}
