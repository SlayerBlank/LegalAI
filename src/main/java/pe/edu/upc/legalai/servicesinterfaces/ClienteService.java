package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.schemas.dtos.request.ClienteRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.ClienteResponseDTO;

import java.util.List;

public interface ClienteService {

    ClienteResponseDTO registrar(ClienteRequestDTO request);

    List<ClienteResponseDTO> listarPorUsuarioAutenticado();

    ClienteResponseDTO buscarPorId(Long id);

    ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request);

    void eliminar(Long id);
}
