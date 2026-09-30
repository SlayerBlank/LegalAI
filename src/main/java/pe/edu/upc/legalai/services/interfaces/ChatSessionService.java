package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.chatsession.ChatSessionRequestDTO;
import pe.edu.upc.legalai.dtos.chatsession.ChatSessionResponseDTO;

import java.util.List;

public interface ChatSessionService {

    ChatSessionResponseDTO create(ChatSessionRequestDTO request);

    List<ChatSessionResponseDTO> findAll();

    ChatSessionResponseDTO findById(Long id);

    List<ChatSessionResponseDTO> findByCaseId(Long caseId);

    List<ChatSessionResponseDTO> findByUserId(Long userId);

    ChatSessionResponseDTO update(Long id, ChatSessionRequestDTO request);

    void delete(Long id);
}
