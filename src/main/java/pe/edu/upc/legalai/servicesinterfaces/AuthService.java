package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.AuthLoginRequestDTO;
import pe.edu.upc.legalai.DTOs.request.AuthRegisterRequestDTO;
import pe.edu.upc.legalai.DTOs.response.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO register(AuthRegisterRequestDTO request);

    AuthResponseDTO login(AuthLoginRequestDTO request);
}
