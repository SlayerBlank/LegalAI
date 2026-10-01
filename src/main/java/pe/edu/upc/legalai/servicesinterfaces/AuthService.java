package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.AuthLoginRequestDTO;
import pe.edu.upc.legalai.dtos.request.AuthRegisterRequestDTO;
import pe.edu.upc.legalai.dtos.response.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO register(AuthRegisterRequestDTO request);

    AuthResponseDTO login(AuthLoginRequestDTO request);
}
