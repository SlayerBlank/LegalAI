package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.client.ClientRequestDTO;
import pe.edu.upc.legalai.dtos.client.ClientResponseDTO;

import java.util.List;

public interface ClientService {

    ClientResponseDTO create(ClientRequestDTO request);

    List<ClientResponseDTO> findAll();

    ClientResponseDTO findById(Long id);

    List<ClientResponseDTO> findByOwnerUserId(Long userId);

    ClientResponseDTO update(Long id, ClientRequestDTO request);

    void delete(Long id);
}
