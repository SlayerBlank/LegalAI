package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.DTOs.response.RAGResponseDTO;
import pe.edu.upc.legalai.DTOs.response.RAGSourceDTO;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.entities.SesionChat;
import pe.edu.upc.legalai.entities.SenderType;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.MensajesRepository;
import pe.edu.upc.legalai.repositories.SesionChatRepository;
import pe.edu.upc.legalai.servicesimplements.ChatStore;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatStoreTest {

    final SesionChatRepository sessions = mock(SesionChatRepository.class);
    final MensajesRepository messages = mock(MensajesRepository.class);
    final ChatStore store = new ChatStore(sessions, messages, new ObjectMapper());

    @Test
    void persistsAnAssistantReplyWithItsExactUserMessageAndRetrievalMetadata() {
        SesionChat session = new SesionChat();
        session.setId(12L);
        Mensajes userMessage = new Mensajes();
        userMessage.setMessageId(101L);
        userMessage.setSenderType(SenderType.USER);
        userMessage.setContent("pregunta");
        when(sessions.findById(12L)).thenReturn(Optional.of(session));
        when(messages.findByMessageIdAndSesionIdAndSenderType(101L, 12L, SenderType.USER))
                .thenReturn(Optional.of(userMessage));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RAGSourceDTO source = new RAGSourceDTO("F1", 8L, "documento.pdf", 23L, 4,
                "fragmento", 0.2, 100, 110);
        RAGResponseDTO ragResponse = new RAGResponseDTO(
                "Respuesta [F1]", "gemini", "modelo-x", 1, "CONSULTED_FRAGMENTS", List.of(source));

        Mensajes saved = store.guardarAsistente(12L, 101L, ragResponse.answer(), ragResponse);

        assertThat(saved.getRespuestaA()).isSameAs(userMessage);
        assertThat(saved.getSenderType()).isEqualTo(SenderType.ASSISTANT);
        var metadata = store.metadata(saved);
        assertThat(metadata.provider()).isEqualTo("gemini");
        assertThat(metadata.model()).isEqualTo("modelo-x");
        assertThat(metadata.retrievedChunks()).isEqualTo(1);
        assertThat(metadata.sourcesType()).isEqualTo("CONSULTED_FRAGMENTS");
        assertThat(metadata.sources()).containsExactly(source);
        verify(messages).findByMessageIdAndSesionIdAndSenderType(101L, 12L, SenderType.USER);
        verify(sessions).save(session);
    }

    @Test
    void refusesToLinkAnAssistantMessageToAnotherSessionsUserMessage() {
        SesionChat session = new SesionChat();
        session.setId(12L);
        when(sessions.findById(12L)).thenReturn(Optional.of(session));
        when(messages.findByMessageIdAndSesionIdAndSenderType(101L, 12L, SenderType.USER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> store.guardarAsistente(12L, 101L, "answer",
                new RAGResponseDTO("answer", "gemini", "model", 0, "CONSULTED_FRAGMENTS", List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
