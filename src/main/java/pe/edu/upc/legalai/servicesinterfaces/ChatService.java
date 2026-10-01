package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.DTOs.request.ChatCreateSessionRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ChatSendMessageRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ChatUpdateSessionRequestDTO;
import pe.edu.upc.legalai.DTOs.response.ChatMessageDTO;
import pe.edu.upc.legalai.DTOs.response.ChatMessageResponseDTO;
import pe.edu.upc.legalai.DTOs.response.ChatSessionResponseDTO;

import java.util.List;

public interface ChatService {

    ChatSessionResponseDTO crear(ChatCreateSessionRequestDTO request);

    List<ChatSessionResponseDTO> listar(Long caseId, Integer page, Integer size);

    ChatSessionResponseDTO obtener(Long sessionId);

    ChatMessageResponseDTO enviarMensaje(Long sessionId, ChatSendMessageRequestDTO request);

    List<ChatMessageDTO> historial(Long sessionId, Integer page, Integer size);

    ChatSessionResponseDTO actualizarTitulo(Long sessionId, ChatUpdateSessionRequestDTO request);

    void eliminar(Long sessionId);
}