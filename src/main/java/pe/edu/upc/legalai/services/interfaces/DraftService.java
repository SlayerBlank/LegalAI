package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.draft.DraftRequestDTO;
import pe.edu.upc.legalai.dtos.draft.DraftResponseDTO;

import java.util.List;

public interface DraftService {

    DraftResponseDTO create(DraftRequestDTO request);

    List<DraftResponseDTO> findAll();

    DraftResponseDTO findById(Long id);

    List<DraftResponseDTO> findByCaseId(Long caseId);

    DraftResponseDTO update(Long id, DraftRequestDTO request);

    void delete(Long id);
}
