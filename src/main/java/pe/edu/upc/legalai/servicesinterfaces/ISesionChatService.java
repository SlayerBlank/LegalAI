package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.SesionChatRequestDTO;
import pe.edu.upc.legalai.dtos.response.SesionChatResponseDTO;

import java.util.List;

public interface ISesionChatService {
    SesionChatResponseDTO registrar(Long expedienteId, SesionChatRequestDTO request);
    List<SesionChatResponseDTO> listarPorExpediente(Long expedienteId);
    List<SesionChatResponseDTO> listarPorUsuarioAutenticado();
    SesionChatResponseDTO buscarPorId(Long id);
    SesionChatResponseDTO actualizar(Long id, SesionChatRequestDTO request);
    void eliminar(Long id);
}