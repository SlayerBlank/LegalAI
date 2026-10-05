package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.SummariesRequestDTO;
import pe.edu.upc.legalai.dtos.response.SummariesResponseDTO;

import java.util.List;

public interface ISummariesService {

    SummariesResponseDTO registrar(SummariesRequestDTO request);

    List<SummariesResponseDTO> listarPorUsuarioAutenticado();

    SummariesResponseDTO buscarPorId(Long id);

    SummariesResponseDTO actualizar(
            Long id,
            SummariesRequestDTO request
    );

    void eliminar(Long id);
}