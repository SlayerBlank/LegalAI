package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesimplements.RAGAuditService;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RAGAuditServiceTest {

    private AuditLogService auditLogService;
    private RAGAuditService service;
    private Usuario usuario;

    @BeforeEach
    void setup() {
        auditLogService = mock(AuditLogService.class);
        service = new RAGAuditService(auditLogService);
        usuario = new Usuario();
        usuario.setUserId(1L);
    }

    @Test
    void registrarConConsultaExitosaIncluyeResultSuccessEnLosDetalles() {
        service.registrar(usuario, "CASE", 5L, 3, true);

        verify(auditLogService).registrar(eq(usuario), eq("RAG_QUERY"), eq("CASE"), eq(5L),
                eq("queryType=CASE;retrievedChunks=3;result=SUCCESS"));
    }

    @Test
    void registrarConConsultaFallidaIncluyeResultFailedEnLosDetalles() {
        service.registrar(usuario, "DOCUMENT", 7L, 0, false);

        verify(auditLogService).registrar(eq(usuario), eq("RAG_QUERY"), eq("DOCUMENT"), eq(7L),
                eq("queryType=DOCUMENT;retrievedChunks=0;result=FAILED"));
    }
}
