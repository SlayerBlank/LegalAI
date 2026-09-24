package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.CitacionIARequestDTO;
import pe.edu.upc.legalai.DTOs.response.CitacionIAResponseDTO;

import java.util.List;

public interface CitacionesIAService {

    CitacionIAResponseDTO crear(CitacionIARequestDTO request);

    List<CitacionIAResponseDTO> listar();

    List<CitacionIAResponseDTO> listarPorMensaje(Long messageId);

    CitacionIAResponseDTO buscarPorId(Long id);

    CitacionIAResponseDTO actualizar(Long id, CitacionIARequestDTO request);

    void eliminar(Long id);
}
