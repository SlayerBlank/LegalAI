package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.dtos.response.RAGSourceDTO;
import pe.edu.upc.legalai.entities.Mensajes;
import java.util.List;

public interface ICitacionesIAService {
    void registrar(Mensajes respuesta, List<RAGSourceDTO> sources);
    List<CitacionIAResponseDTO> listarPorMensaje(Long sessionId, Long messageId);
}
