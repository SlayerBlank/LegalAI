package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.document.DocumentRequestDTO;
import pe.edu.upc.legalai.dtos.document.DocumentResponseDTO;

import java.util.List;

public interface DocumentService {

    DocumentResponseDTO create(DocumentRequestDTO request);

    List<DocumentResponseDTO> findAll();

    DocumentResponseDTO findById(Long id);

    List<DocumentResponseDTO> findByUploadedByUserId(Long userId);

    DocumentResponseDTO update(Long id, DocumentRequestDTO request);

    void delete(Long id);
}
