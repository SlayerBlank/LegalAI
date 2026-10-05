package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.dtos.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.entities.EstadoExpediente;

import java.time.LocalDate;
import java.util.List;

public interface IExpedienteService {

    ExpedienteResponseDTO registrar(ExpedienteRequestDTO request);

    List<ExpedienteResponseDTO> listarPorUsuarioAutenticado();

    List<ExpedienteResponseDTO> listarPorCliente(Long clientId);

    List<ExpedienteResponseDTO> listarPorEstadoYFecha(EstadoExpediente status, LocalDate openedFrom, LocalDate openedTo);

    ExpedienteResponseDTO buscarPorId(Long id);

    ExpedienteResponseDTO actualizar(Long id, ExpedienteRequestDTO request);

    void eliminar(Long id);
}
