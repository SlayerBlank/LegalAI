package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.casefile.CaseFileRequestDTO;
import pe.edu.upc.legalai.dtos.casefile.CaseFileResponseDTO;

import java.util.List;

public interface CaseFileService {

    CaseFileResponseDTO create(CaseFileRequestDTO request);

    List<CaseFileResponseDTO> findAll();

    CaseFileResponseDTO findById(Long id);

    List<CaseFileResponseDTO> findByClientId(Long clientId);

    List<CaseFileResponseDTO> findByOwnerUserId(Long userId);

    CaseFileResponseDTO update(Long id, CaseFileRequestDTO request);

    void delete(Long id);
}
