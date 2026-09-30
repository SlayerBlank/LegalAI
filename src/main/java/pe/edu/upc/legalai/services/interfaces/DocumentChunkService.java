package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.documentchunk.DocumentChunkRequestDTO;
import pe.edu.upc.legalai.dtos.documentchunk.DocumentChunkResponseDTO;

import java.util.List;

public interface DocumentChunkService {

    DocumentChunkResponseDTO create(DocumentChunkRequestDTO request);

    List<DocumentChunkResponseDTO> findAll();

    DocumentChunkResponseDTO findById(Long id);

    List<DocumentChunkResponseDTO> findByDocumentId(Long documentId);

    void delete(Long id);
}
