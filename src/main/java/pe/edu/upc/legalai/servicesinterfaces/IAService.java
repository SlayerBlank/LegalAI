package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.IARequestDTO;
import pe.edu.upc.legalai.DTOs.request.IAContextRequestDTO;
import pe.edu.upc.legalai.DTOs.response.IAResponseDTO;

public interface IAService {
    IAResponseDTO generarRespuesta(IARequestDTO request);
    IAResponseDTO generarRespuestaDocumental(IAContextRequestDTO request);
}
