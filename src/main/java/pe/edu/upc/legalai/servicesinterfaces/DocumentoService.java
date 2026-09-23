package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.schemas.dtos.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.schemas.dtos.response.DocumentoResponseDTO;

import java.util.List;

public interface DocumentoService {

    DocumentoResponseDTO registrar(Long caseId, DocumentoRequestDTO request);

    List<DocumentoResponseDTO> listarPorExpediente(Long caseId);

    DocumentoResponseDTO buscarPorId(Long id);

    void eliminar(Long id);
}
