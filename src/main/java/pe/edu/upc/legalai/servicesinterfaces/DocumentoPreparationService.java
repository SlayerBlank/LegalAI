package pe.edu.upc.legalai.servicesinterfaces;

import org.springframework.web.multipart.MultipartFile;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoPreparationResponseDTO;

public interface DocumentoPreparationService {
    DocumentoResponseDTO subirYPreparar(Long caseId, MultipartFile file, String category);
    DocumentoPreparationResponseDTO preparar(Long documentId);
    DocumentoPreparationResponseDTO estado(Long documentId);
}
