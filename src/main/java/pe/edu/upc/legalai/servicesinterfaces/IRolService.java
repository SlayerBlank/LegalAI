package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.response.RolResponseDTO;
import pe.edu.upc.legalai.dtos.request.RolRequestDTO;

import java.util.List;

public interface IRolService {

    List<RolResponseDTO> listar();

    RolResponseDTO registrar(RolRequestDTO request);

    RolResponseDTO buscarPorId(Long id);

    RolResponseDTO actualizar(Long id, RolRequestDTO request);

    void eliminar(Long id);
}
