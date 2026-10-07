package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesimplements.ChatAuditService;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatAuditServiceTest {

    private AuditLogService auditLogService;
    private ChatAuditService service;
    private Usuario usuario;

    @BeforeEach
    void setup() {
        auditLogService = mock(AuditLogService.class);
        service = new ChatAuditService(auditLogService);
        usuario = new Usuario();
        usuario.setUserId(1L);
    }

    @Test
    void registrarDelegaEnAuditLogServiceConEntityTypeChatSession() {
        service.registrar(usuario, "CREATE_CHAT_SESSION", 10L, "detalle");

        verify(auditLogService).registrar(eq(usuario), eq("CREATE_CHAT_SESSION"), eq("ChatSession"), eq(10L), eq("detalle"));
    }

    @Test
    void registrarFalloDelegaEnAuditLogServiceConEntityTypeChatSession() {
        service.registrarFallo(usuario, "CHAT_MESSAGE_FAILED", 10L, "error de IA");

        verify(auditLogService).registrar(eq(usuario), eq("CHAT_MESSAGE_FAILED"), eq("ChatSession"), eq(10L), eq("error de IA"));
    }
}
