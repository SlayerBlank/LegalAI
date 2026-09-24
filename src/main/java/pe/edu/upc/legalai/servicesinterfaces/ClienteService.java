package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.ClienteRequestDTO;
import pe.edu.upc.legalai.DTOs.response.ClienteResponseDTO;

import java.util.List;

public interface ClienteService {

    ClienteResponseDTO registrar(ClienteRequestDTO request);

    List<ClienteResponseDTO> listarPorUsuarioAutenticado();

    ClienteResponseDTO buscarPorId(Long id);

    ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request);

    void eliminar(Long id);
}
