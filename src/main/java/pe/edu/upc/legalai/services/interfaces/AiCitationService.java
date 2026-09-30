package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.aicitation.AiCitationRequestDTO;
import pe.edu.upc.legalai.dtos.aicitation.AiCitationResponseDTO;

import java.util.List;

public interface AiCitationService {

    AiCitationResponseDTO create(AiCitationRequestDTO request);

    List<AiCitationResponseDTO> findAll();

    AiCitationResponseDTO findById(Long id);

    List<AiCitationResponseDTO> findByMessageId(Long messageId);

    void delete(Long id);
}
