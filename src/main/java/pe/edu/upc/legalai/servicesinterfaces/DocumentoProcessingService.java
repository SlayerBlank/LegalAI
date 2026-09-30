package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.DTOs.response.DocumentoTextResponseDTO;

public interface DocumentoProcessingService {
    DocumentoResponseDTO procesar(Long documentId);
    DocumentoTextResponseDTO obtenerTexto(Long documentId);
}
