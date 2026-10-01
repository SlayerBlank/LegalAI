package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.ChatHistoryTurnDTO;
import pe.edu.upc.legalai.dtos.request.RAGRequestDTO;
import pe.edu.upc.legalai.dtos.response.RAGResponseDTO;

import java.util.List;

public interface RAGService {
    RAGResponseDTO preguntarDocumento(Long documentId, RAGRequestDTO request);
    RAGResponseDTO preguntarExpediente(Long caseId, RAGRequestDTO request);
    RAGResponseDTO preguntarDocumentoConversacional(Long documentId, RAGRequestDTO request,
                                                     List<ChatHistoryTurnDTO> historial);
    RAGResponseDTO preguntarExpedienteConversacional(Long caseId, RAGRequestDTO request,
                                                      List<ChatHistoryTurnDTO> historial);
}