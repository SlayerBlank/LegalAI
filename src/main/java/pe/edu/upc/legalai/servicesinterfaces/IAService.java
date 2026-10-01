package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.IARequestDTO;
import pe.edu.upc.legalai.dtos.request.IAContextRequestDTO;
import pe.edu.upc.legalai.dtos.response.IAResponseDTO;

public interface IAService {
    IAResponseDTO generarRespuesta(IARequestDTO request);
    IAResponseDTO generarRespuestaDocumental(IAContextRequestDTO request);
}
