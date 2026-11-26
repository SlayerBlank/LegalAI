package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.dtos.request.DocumentoUpdateRequestDTO;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Optional;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.repositories.IUsuarioRepository;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.servicesimplements.DocumentStorage;
import pe.edu.upc.legalai.servicesimplements.DocumentoServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DocumentoServiceImplTest {

    private IDocumentoRepository documentoRepository;
    private IExpedienteRepository expedienteRepository;
    private IUsuarioRepository usuarioRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private DocumentStorage documentStorage;
    private DocumentoServiceImplement service;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        documentoRepository = mock(IDocumentoRepository.class);
        expedienteRepository = mock(IExpedienteRepository.class);
        usuarioRepository = mock(IUsuarioRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        documentStorage = mock(DocumentStorage.class);
        service = new DocumentoServiceImplement(documentoRepository, expedienteRepository, usuarioRepository,
                usuarioService, auditLogService, documentStorage, DataSize.ofMegabytes(10));

        usuario = new Usuario();
        usuario.setUserId(1L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(usuarioRepository.existsById(1L)).thenReturn(true);
    }

    private Documento documento(Long id, EstadoProcesamiento status) {
        Expediente expediente = new Expediente();
        expediente.setCaseId(5L);
        expediente.setTitle("Desalojo Lopez");
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

    @Test
    void listarPendientesRevisionDeOtroAbogadoSinSerAdminDevuelve404() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);

        assertThrows(ResourceNotFoundException.class, () -> service.listarPendientesRevision(2L, null));
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void listarPendientesRevisionDeOtroAbogadoSiendoAdminEstaPermitido() {
        Rol admin = new Rol();
        admin.setName("ADMIN");
        usuario.setRol(admin);
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(documentoRepository.findByExpedienteOwnerUserIdAndProcessingStatusInOrderByCreatedAtAsc(eq(2L), any()))
                .thenReturn(List.of(documento(4L, EstadoProcesamiento.ERROR)));

        assertEquals(1, service.listarPendientesRevision(2L, null).size());
    }

    @Test
    void eliminarBorraTambienElPdfVerificadoDelStorage() throws Exception {
        Path pdf = Files.createTempFile("legalai-test", ".pdf");
        Documento existente = documento(8L, EstadoProcesamiento.PROCESSED);
        existente.setStorageUrl(pdf.getFileName().toString());
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(8L, 1L)).thenReturn(java.util.Optional.of(existente));
        when(documentStorage.resolve(existente.getStorageUrl())).thenReturn(pdf);

        service.eliminar(8L);

        verify(documentoRepository).delete(existente);
        assertFalse(Files.exists(pdf));
    }

    @Test
    void eliminarConservaElArchivoSiNoTieneCargaVerificable() throws Exception {
        Path pdf = Files.createTempFile("legalai-test", ".pdf");
        try {
            Documento existente = documento(9L, EstadoProcesamiento.UPLOADED);
            existente.setStorageUrl(pdf.getFileName().toString());
            when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(9L, 1L)).thenReturn(java.util.Optional.of(existente));
            doThrow(new ResourceNotFoundException("sin carga")).when(documentStorage).verifyUpload(existente);
            when(documentStorage.resolve(any())).thenReturn(pdf);

            service.eliminar(9L);

            verify(documentoRepository).delete(existente);
            assertTrue(Files.exists(pdf));
        } finally {
            Files.deleteIfExists(pdf);
        }
    }

    // ---------- HU-67

    @Test
    void listarPendientesRevisionIncluyeElTituloDelExpediente() {
        when(documentoRepository.findByExpedienteOwnerUserIdAndProcessingStatusInOrderByCreatedAtAsc(eq(1L), any()))
                .thenReturn(List.of(documento(3L, EstadoProcesamiento.UPLOADED)));

        var result = service.listarPendientesRevision(1L, null);

        assertEquals("Desalojo Lopez", result.get(0).getCaseTitle());
        assertEquals("contrato-3.pdf", result.get(0).getFileName());
    }

    @Test
    void listarPendientesRevisionConEstadoNoPendienteLanzaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.listarPendientesRevision(1L, EstadoProcesamiento.PROCESSED));
        verifyNoInteractions(documentoRepository);
    }

    // ---------- HU-025

    @Test
    void actualizarCambiaSoloLaCategoriaDelDocumentoPropio() {
        Documento existente = documento(7L, EstadoProcesamiento.PROCESSED);
        existente.setStorageUrl("archivo.pdf");
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L)).thenReturn(Optional.of(existente));
        when(documentoRepository.saveAndFlush(existente)).thenReturn(existente);
        DocumentoUpdateRequestDTO request = new DocumentoUpdateRequestDTO();
        request.setCategory("  Contratos  ");

        var result = service.actualizar(7L, request);

        assertEquals("Contratos", result.getCategory());
        assertEquals("contrato-7.pdf", result.getFileName());
        assertEquals("archivo.pdf", existente.getStorageUrl());
        verify(auditLogService).registrar(eq(usuario), eq("UPDATE_DOCUMENT"), eq("Documento"), eq(7L), any());
    }

    @Test
    void actualizarConCategoriaVaciaLaElimina() {
        Documento existente = documento(7L, EstadoProcesamiento.PROCESSED);
        existente.setCategory("Antigua");
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L)).thenReturn(Optional.of(existente));
        when(documentoRepository.saveAndFlush(existente)).thenReturn(existente);
        DocumentoUpdateRequestDTO request = new DocumentoUpdateRequestDTO();
        request.setCategory("   ");

        assertNull(service.actualizar(7L, request).getCategory());
    }

    @Test
    void actualizarDocumentoAjenoLanzaResourceNotFoundYNoGuarda() {
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(8L, new DocumentoUpdateRequestDTO()));
        verify(documentoRepository, never()).saveAndFlush(any());
    }

    // ---------- HU-022

    private Expediente expedientePropio() {
        Expediente expediente = new Expediente();
        expediente.setCaseId(5L);
        when(expedienteRepository.findByCaseIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(expediente));
        return expediente;
    }

    @Test
    void subirArchivoGuardaElPdfMedianteDocumentStorageYAsociaElExpediente() throws Exception {
        expedientePropio();
        byte[] pdf = "%PDF-1.7\ncontenido".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdf);
        when(documentStorage.store(any(), any(), anyLong()))
                .thenReturn(new DocumentStorage.StoredFile("uuid.pdf", Path.of("uuid.pdf"), pdf.length));
        when(documentoRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            d.setDocumentId(20L);
            return d;
        });

        var result = service.subirArchivo(5L, file, "Contratos");

        assertEquals(20L, result.getDocumentId());
        assertEquals(5L, result.getCaseId());
        assertEquals("uuid.pdf", result.getStorageUrl());
        assertEquals(EstadoProcesamiento.UPLOADED, result.getProcessingStatus());
        verify(documentStorage).store(any(), any(), eq(DataSize.ofMegabytes(10).toBytes()));
    }

    @Test
    void subirArchivoQueNoEsPdfLanzaBadRequestSinGuardar() throws Exception {
        expedientePropio();
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.subirArchivo(5L, file, null));

        assertEquals("Solo se permiten archivos PDF con Content-Type application/pdf", ex.getMessage());
        verify(documentStorage, never()).store(any(), any(), anyLong());
    }

    @Test
    void subirArchivoConCabeceraFalsaLanzaBadRequestSinGuardar() throws Exception {
        expedientePropio();
        MockMultipartFile file = new MockMultipartFile("file", "falso.pdf", "application/pdf", "no es un pdf".getBytes());

        assertThrows(BadRequestException.class, () -> service.subirArchivo(5L, file, null));
        verify(documentStorage, never()).store(any(), any(), anyLong());
    }

    @Test
    void subirArchivoAExpedienteAjenoLanzaResourceNotFound() throws Exception {
        when(expedienteRepository.findByCaseIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF-1.7 x".getBytes());

        assertThrows(ResourceNotFoundException.class, () -> service.subirArchivo(6L, file, null));
        verify(documentStorage, never()).store(any(), any(), anyLong());
    }
}
