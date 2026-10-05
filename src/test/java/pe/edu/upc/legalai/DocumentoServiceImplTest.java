package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.repositories.IUsuarioRepository;
import pe.edu.upc.legalai.servicesimplements.DocumentoServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DocumentoServiceImplTest {

    private IDocumentoRepository documentoRepository;
    private IExpedienteRepository expedienteRepository;
    private IUsuarioRepository usuarioRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private DocumentoServiceImplement service;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        documentoRepository = mock(IDocumentoRepository.class);
        expedienteRepository = mock(IExpedienteRepository.class);
        usuarioRepository = mock(IUsuarioRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        service = new DocumentoServiceImplement(documentoRepository, expedienteRepository, usuarioRepository,
                usuarioService, auditLogService, System.getProperty("java.io.tmpdir"), DataSize.ofMegabytes(10));

        usuario = new Usuario();
        usuario.setUserId(1L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(usuarioRepository.existsById(1L)).thenReturn(true);
    }

    private Documento documento(Long id, EstadoProcesamiento status) {
        Expediente expediente = new Expediente();
        expediente.setCaseId(5L);
        Documento documento = new Documento();
        documento.setDocumentId(id);
        documento.setExpediente(expediente);
        documento.setFileName("contrato-" + id + ".pdf");
        documento.setProcessingStatus(status);
        return documento;
    }

    @Test
    void listarPendientesRevisionSinFiltroUsaUploadedProcessingYError() {
        when(documentoRepository.findByExpedienteOwnerUserIdAndProcessingStatusInOrderByCreatedAtAsc(
                eq(1L), eq(List.of(EstadoProcesamiento.UPLOADED, EstadoProcesamiento.PROCESSING, EstadoProcesamiento.ERROR))))
                .thenReturn(List.of(documento(1L, EstadoProcesamiento.UPLOADED), documento(2L, EstadoProcesamiento.ERROR)));

        var result = service.listarPendientesRevision(1L, null);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).getCaseId());
        verify(usuarioRepository).existsById(1L);
    }

    @Test
    void listarPendientesRevisionConFiltroConsultaSoloEseEstado() {
        when(documentoRepository.findByExpedienteOwnerUserIdAndProcessingStatusInOrderByCreatedAtAsc(
                eq(1L), eq(List.of(EstadoProcesamiento.PROCESSING))))
                .thenReturn(List.of(documento(3L, EstadoProcesamiento.PROCESSING)));

        var result = service.listarPendientesRevision(1L, EstadoProcesamiento.PROCESSING);

        assertEquals(1, result.size());
        assertEquals(EstadoProcesamiento.PROCESSING, result.get(0).getProcessingStatus());
    }

    @Test
    void listarPendientesRevisionSinResultadosDevuelveListaVacia() {
        when(documentoRepository.findByExpedienteOwnerUserIdAndProcessingStatusInOrderByCreatedAtAsc(eq(1L), any()))
                .thenReturn(List.of());

        assertTrue(service.listarPendientesRevision(1L, null).isEmpty());
    }

    @Test
    void listarPendientesRevisionConAbogadoInexistenteLanzaResourceNotFound() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.listarPendientesRevision(99L, null));
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void eliminarBorraElDocumentoPropio() {
        Documento existente = documento(7L, EstadoProcesamiento.UPLOADED);
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L)).thenReturn(java.util.Optional.of(existente));

        service.eliminar(7L);

        verify(documentoRepository).delete(existente);
    }
}
