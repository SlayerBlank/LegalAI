package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.DTOs.response.ExpedienteResponseDTO;

import java.util.List;

public interface ExpedienteService {

    ExpedienteResponseDTO registrar(ExpedienteRequestDTO request);

    List<ExpedienteResponseDTO> listarPorUsuarioAutenticado();

    List<ExpedienteResponseDTO> listarPorCliente(Long clientId);

    ExpedienteResponseDTO buscarPorId(Long id);

    ExpedienteResponseDTO actualizar(Long id, ExpedienteRequestDTO request);

    void eliminar(Long id);
}
