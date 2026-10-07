package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.request.SummariesRequestDTO;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Summaries;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.repositories.ISummariesRepository;
import pe.edu.upc.legalai.servicesimplements.SummariesServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SummariesServiceImplTest {

    private ISummariesRepository summariesRepository;
    private IDocumentoRepository documentoRepository;
    private IExpedienteRepository expedienteRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private SummariesServiceImplement service;

    private Usuario usuario;
    private Usuario otroUsuario;
    private Expediente expediente;
    private Documento documento;

    @BeforeEach
    void setup() {
        summariesRepository = mock(ISummariesRepository.class);
        documentoRepository = mock(IDocumentoRepository.class);
        expedienteRepository = mock(IExpedienteRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        service = new SummariesServiceImplement(summariesRepository, documentoRepository, expedienteRepository,
                usuarioService, auditLogService);

        usuario = new Usuario();
        usuario.setUserId(1L);
        otroUsuario = new Usuario();
        otroUsuario.setUserId(99L);

        expediente = new Expediente();
        expediente.setCaseId(5L);
        expediente.setOwner(usuario);

        documento = new Documento();
        documento.setDocumentId(10L);
        documento.setExpediente(expediente);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(summariesRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Summaries s = inv.getArgument(0);
            if (s.getSummaryId() == null) s.setSummaryId(50L);
            return s;
        });
    }

    private SummariesRequestDTO request(Long documentId, Long caseId) {
        SummariesRequestDTO request = new SummariesRequestDTO();
        request.setDocumentId(documentId);
        request.setCaseId(caseId);
        request.setSummaryType("AI_GENERATED");
        request.setContent("Resumen del caso");
        return request;
    }

    @Test
    void registrarSinDocumentIdNiCaseIdLanzaBadRequest() {
        assertThrows(BadRequestException.class, () -> service.registrar(request(null, null)));
        verifyNoInteractions(summariesRepository);
    }

    @Test
    void registrarConDocumentoAjenoLanzaResourceNotFound() {
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request(10L, null)));
    }

    @Test
    void registrarConExpedienteAjenoLanzaResourceNotFound() {
        when(expedienteRepository.findByCaseIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request(null, 5L)));
    }

    @Test
    void registrarConDocumentoQueNoPerteneceAlExpedienteLanzaBadRequest() {
        Expediente otroExpediente = new Expediente();
        otroExpediente.setCaseId(6L);
        otroExpediente.setOwner(usuario);
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(10L, 1L)).thenReturn(Optional.of(documento));
        when(expedienteRepository.findByCaseIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(otroExpediente));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.registrar(request(10L, 6L)));
        assertTrue(ex.getMessage().contains("no pertenece al expediente"));
    }

    @Test
    void registrarSoloConCaseIdCreaElResumenYAudita() {
        when(expedienteRepository.findByCaseIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(expediente));

        var response = service.registrar(request(null, 5L));

        assertEquals(50L, response.getSummaryId());
        assertEquals(5L, response.getCaseId());
        assertNull(response.getDocumentId());
        verify(auditLogService).registrar(eq(usuario), eq("CREATE_SUMMARY"), eq("Summaries"), eq(50L), any());
    }

    @Test
    void registrarSoloConDocumentIdCreaElResumen() {
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(10L, 1L)).thenReturn(Optional.of(documento));

        var response = service.registrar(request(10L, null));

        assertEquals(10L, response.getDocumentId());
        assertNull(response.getCaseId());
    }

    @Test
    void listarPorUsuarioAutenticadoExcluyeResumenesSinRelacionesPropias() {
        Summaries huerfano = new Summaries();
        huerfano.setSummaryId(1L);
        huerfano.setGeneratedBy(usuario);

        Summaries valido = new Summaries();
        valido.setSummaryId(2L);
        valido.setGeneratedBy(usuario);
        valido.setExpediente(expediente);

        when(summariesRepository.findByGeneratedByUserIdOrderBySummaryIdDesc(1L))
                .thenReturn(List.of(huerfano, valido));

        var result = service.listarPorUsuarioAutenticado();

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getSummaryId());
    }

    @Test
    void buscarPorIdConIdInvalidoLanzaBadRequest() {
        assertThrows(BadRequestException.class, () -> service.buscarPorId(0L));
        verifyNoInteractions(summariesRepository);
    }

    @Test
    void buscarPorIdInexistenteLanzaResourceNotFound() {
        when(summariesRepository.findBySummaryIdAndGeneratedByUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(1L));
    }

    @Test
    void buscarPorIdConExpedienteDeOtroDuenoLanzaResourceNotFound() {
        Expediente expedienteAjeno = new Expediente();
        expedienteAjeno.setCaseId(5L);
        expedienteAjeno.setOwner(otroUsuario);
        Summaries summary = new Summaries();
        summary.setSummaryId(1L);
        summary.setGeneratedBy(usuario);
        summary.setExpediente(expedienteAjeno);
        when(summariesRepository.findBySummaryIdAndGeneratedByUserId(1L, 1L)).thenReturn(Optional.of(summary));

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(1L));
    }

    @Test
    void actualizarModificaLosDatosYRegistraAuditoria() {
        Summaries existente = new Summaries();
        existente.setSummaryId(1L);
        existente.setGeneratedBy(usuario);
        existente.setExpediente(expediente);
        when(summariesRepository.findBySummaryIdAndGeneratedByUserId(1L, 1L)).thenReturn(Optional.of(existente));
        when(expedienteRepository.findByCaseIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(expediente));

        var response = service.actualizar(1L, request(null, 5L));

        assertEquals("Resumen del caso", response.getContent());
        verify(auditLogService).registrar(eq(usuario), eq("UPDATE_SUMMARY"), eq("Summaries"), eq(1L), any());
    }

    @Test
    void eliminarBorraElResumenPropioYRegistraAuditoria() {
        Summaries existente = new Summaries();
        existente.setSummaryId(1L);
        existente.setGeneratedBy(usuario);
        existente.setExpediente(expediente);
        when(summariesRepository.findBySummaryIdAndGeneratedByUserId(1L, 1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(summariesRepository).delete(existente);
        verify(auditLogService).registrar(eq(usuario), eq("DELETE_SUMMARY"), eq("Summaries"), eq(1L), any());
    }
}
