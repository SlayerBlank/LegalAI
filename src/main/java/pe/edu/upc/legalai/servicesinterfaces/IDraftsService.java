package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.DraftsRequestDTO;
import pe.edu.upc.legalai.dtos.response.DraftsConsultaResponseDTO;
import pe.edu.upc.legalai.dtos.response.DraftsResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface IDraftsService {

    DraftsResponseDTO registrar(DraftsRequestDTO request);

    List<DraftsResponseDTO> listarPorUsuarioAutenticado();

    DraftsResponseDTO buscarPorId(Long id);

    DraftsResponseDTO actualizar(
            Long id,
            DraftsRequestDTO request
    );

    void eliminar(Long id);

    List<DraftsConsultaResponseDTO> consultarPorExpedienteEstadoYFecha(
            Long caseId,
            String status,
            LocalDate updatedFrom,
            LocalDate updatedTo
    );
}