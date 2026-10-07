package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.request.DraftsRequestDTO;
import pe.edu.upc.legalai.entities.Drafts;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDraftsRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.servicesimplements.DraftsServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DraftsServiceImplTest {

    private IDraftsRepository draftsRepository;
    private IExpedienteRepository expedienteRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private DraftsServiceImplement service;

    private Usuario usuario;
    private Expediente expediente;

    @BeforeEach
    void setup() {
        draftsRepository = mock(IDraftsRepository.class);
        expedienteRepository = mock(IExpedienteRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        service = new DraftsServiceImplement(draftsRepository, expedienteRepository, usuarioService, auditLogService);

        usuario = new Usuario();
        usuario.setUserId(1L);
        expediente = new Expediente();
        expediente.setCaseId(5L);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(expedienteRepository.findByCaseIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(expediente));
        when(draftsRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Drafts d = inv.getArgument(0);
            if (d.getDraftId() == null) d.setDraftId(100L);
            return d;
        });
    }

    private DraftsRequestDTO request(String titulo) {
        DraftsRequestDTO request = new DraftsRequestDTO();
        request.setCaseId(5L);
        request.setTitle(titulo);
        request.setPrompt("Genera un contrato");
        request.setContent("Contenido del borrador");
        request.setStatus("DRAFT");
        return request;
    }

    @Test
    void registrarAsociaElExpedientePropioYElUsuarioAutenticado() {
        var response = service.registrar(request("Contrato de arriendo"));

        assertEquals(100L, response.getDraftId());
        assertEquals(5L, response.getCaseId());
        assertEquals(1L, response.getCreatedByUserId());
        assertEquals("Contrato de arriendo", response.getTitle());
        verify(auditLogService).registrar(eq(usuario), eq("CREATE_DRAFT"), eq("Drafts"), eq(100L), any());
    }

    @Test
    void registrarConExpedienteAjenoLanzaResourceNotFound() {
        when(expedienteRepository.findByCaseIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request("Contrato")));
        verifyNoInteractions(draftsRepository);
    }

    @Test
    void listarPorUsuarioAutenticadoDelegaConElUserIdDosVeces() {
        Drafts draft = draftConId(10L);
        when(draftsRepository.findByCreatedByUserIdAndExpedienteOwnerUserIdOrderByDraftIdDesc(1L, 1L))
                .thenReturn(List.of(draft));

        var result = service.listarPorUsuarioAutenticado();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getDraftId());
    }

    @Test
    void buscarPorIdConIdInvalidoLanzaBadRequest() {
        assertThrows(BadRequestException.class, () -> service.buscarPorId(0L));
        assertThrows(BadRequestException.class, () -> service.buscarPorId(null));
        verifyNoInteractions(draftsRepository);
    }

    @Test
    void buscarPorIdInexistenteLanzaResourceNotFound() {
        when(draftsRepository.findByDraftIdAndCreatedByUserIdAndExpedienteOwnerUserId(10L, 1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(10L));
    }

    @Test
    void actualizarModificaLosDatosYRegistraAuditoria() {
        Drafts existente = draftConId(10L);
        when(draftsRepository.findByDraftIdAndCreatedByUserIdAndExpedienteOwnerUserId(10L, 1L, 1L))
                .thenReturn(Optional.of(existente));

        var response = service.actualizar(10L, request("Titulo actualizado"));

        assertEquals("Titulo actualizado", response.getTitle());
        verify(auditLogService).registrar(eq(usuario), eq("UPDATE_DRAFT"), eq("Drafts"), eq(10L), any());
    }

    @Test
    void eliminarBorraElBorradorPropioYRegistraAuditoria() {
        Drafts existente = draftConId(10L);
        when(draftsRepository.findByDraftIdAndCreatedByUserIdAndExpedienteOwnerUserId(10L, 1L, 1L))
                .thenReturn(Optional.of(existente));

        service.eliminar(10L);

        verify(draftsRepository).delete(existente);
        verify(auditLogService).registrar(eq(usuario), eq("DELETE_DRAFT"), eq("Drafts"), eq(10L), any());
    }

    private Drafts draftConId(Long id) {
        Drafts draft = new Drafts();
        draft.setDraftId(id);
        draft.setExpediente(expediente);
        draft.setCreatedBy(usuario);
        draft.setTitle("Titulo");
        draft.setPrompt("Prompt");
        draft.setContent("Contenido");
        draft.setStatus("DRAFT");
        return draft;
    }
}
