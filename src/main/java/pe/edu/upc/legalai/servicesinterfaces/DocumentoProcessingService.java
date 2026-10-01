package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoTextResponseDTO;

public interface DocumentoProcessingService {
    DocumentoResponseDTO procesar(Long documentId);
    DocumentoTextResponseDTO obtenerTexto(Long documentId);
}
