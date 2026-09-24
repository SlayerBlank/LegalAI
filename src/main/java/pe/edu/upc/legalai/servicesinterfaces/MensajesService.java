package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.MensajeRequestDTO;
import pe.edu.upc.legalai.DTOs.response.MensajeResponseDTO;

import java.util.List;

public interface MensajesService {

    MensajeResponseDTO crear(MensajeRequestDTO request);

    List<MensajeResponseDTO> listar();

    MensajeResponseDTO buscarPorId(Long id);

    MensajeResponseDTO actualizar(Long id, MensajeRequestDTO request);

    void eliminar(Long id);
}
