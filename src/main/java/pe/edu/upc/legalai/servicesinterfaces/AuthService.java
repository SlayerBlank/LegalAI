package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.schemas.dtos.request.AuthLoginRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.request.AuthRegisterRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO register(AuthRegisterRequestDTO request);

    AuthResponseDTO login(AuthLoginRequestDTO request);
}
