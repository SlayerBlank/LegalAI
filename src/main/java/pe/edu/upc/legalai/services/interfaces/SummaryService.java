package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.summary.SummaryRequestDTO;
import pe.edu.upc.legalai.dtos.summary.SummaryResponseDTO;

import java.util.List;

public interface SummaryService {

    SummaryResponseDTO create(SummaryRequestDTO request);

    List<SummaryResponseDTO> findAll();

    SummaryResponseDTO findById(Long id);

    List<SummaryResponseDTO> findByDocumentId(Long documentId);

    List<SummaryResponseDTO> findByCaseId(Long caseId);

    void delete(Long id);
}
