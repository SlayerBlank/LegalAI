package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.chatmessage.ChatMessageRequestDTO;
import pe.edu.upc.legalai.dtos.chatmessage.ChatMessageResponseDTO;

import java.util.List;

public interface ChatMessageService {

    ChatMessageResponseDTO create(ChatMessageRequestDTO request);

    List<ChatMessageResponseDTO> findAll();

    ChatMessageResponseDTO findById(Long id);

    List<ChatMessageResponseDTO> findBySessionId(Long sessionId);

    void delete(Long id);
}
