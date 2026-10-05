package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.response.RAGSourceDTO;
import pe.edu.upc.legalai.entities.*;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.*;
import pe.edu.upc.legalai.servicesimplements.CitacionesIAServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CitacionesIAServiceImplTest {

    private ICitacionesIARepository citacionesRepository;
    private IMensajesRepository mensajesRepository;
    private IDocumentoRepository documentoRepository;
    private IDocumentChunkRepository chunkRepository;
    private IUsuarioService usuarioService;
    private CitacionesIAServiceImplement service;

    private Usuario usuario;
    private Expediente expediente;
    private Documento documento;
    private SesionChat sesion;
    private Mensajes respuestaAsistente;

    @BeforeEach
    void setup() {
        citacionesRepository = mock(ICitacionesIARepository.class);
        mensajesRepository = mock(IMensajesRepository.class);
        documentoRepository = mock(IDocumentoRepository.class);
        chunkRepository = mock(IDocumentChunkRepository.class);
        usuarioService = mock(IUsuarioService.class);
        service = new CitacionesIAServiceImplement(citacionesRepository, mensajesRepository, documentoRepository,
                chunkRepository, usuarioService);

        usuario = new Usuario();
        usuario.setUserId(1L);

        expediente = new Expediente();
        expediente.setCaseId(5L);
        expediente.setOwner(usuario);

        documento = new Documento();
        documento.setDocumentId(10L);
        documento.setExpediente(expediente);

        sesion = new SesionChat();
        sesion.setId(20L);
        sesion.setExpediente(expediente);
        sesion.setUsuario(usuario);
        sesion.setDocumentScopeRequired(false);

        respuestaAsistente = new Mensajes();
        respuestaAsistente.setMessageId(30L);
        respuestaAsistente.setSesion(sesion);
        respuestaAsistente.setSenderType(SenderType.ASSISTANT);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
    }

    private RAGSourceDTO fuente(Long documentId, Long chunkId, double distance) {
        return new RAGSourceDTO("ref", documentId, "doc.pdf", chunkId, 0, "extracto", distance, 0, 10);
    }

    private DocumentChunk chunk(Documento doc) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkIndex(0);
        chunk.setDocumento(doc);
        return chunk;
    }

    @Test
    void registrarConMensajeQueNoEsDelAsistenteLanzaResourceNotFound() {
        respuestaAsistente.setSenderType(SenderType.USER);

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarConSesionDeOtroUsuarioLanzaResourceNotFound() {
        Usuario otro = new Usuario();
        otro.setUserId(99L);
        sesion.setUsuario(otro);

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarConFuentesNulasLanzaBadRequest() {
        assertThrows(BadRequestException.class, () -> service.registrar(respuestaAsistente, null));
    }

    @Test
    void registrarConFuenteInvalidaLanzaBadRequest() {
        RAGSourceDTO invalida = new RAGSourceDTO("ref", null, "doc.pdf", 100L, 0, "x", 0.1, 0, 10);

        assertThrows(BadRequestException.class, () -> service.registrar(respuestaAsistente, List.of(invalida)));
    }

    @Test
    void registrarConDistanciaNoFinitaLanzaBadRequest() {
        RAGSourceDTO invalida = new RAGSourceDTO("ref", 10L, "doc.pdf", 100L, 0, "x", Double.NaN, 0, 10);

        assertThrows(BadRequestException.class, () -> service.registrar(respuestaAsistente, List.of(invalida)));
    }

    @Test
    void registrarConDocumentoDeFuenteNoDisponibleLanzaResourceNotFound() {
        when(documentoRepository.findOwnedForProcessing(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarConDocumentoDeOtroExpedienteLanzaResourceNotFound() {
        Expediente otroExpediente = new Expediente();
        otroExpediente.setCaseId(6L);
        Documento documentoAjeno = new Documento();
        documentoAjeno.setDocumentId(10L);
        documentoAjeno.setExpediente(otroExpediente);
        when(documentoRepository.findOwnedForProcessing(10L, 1L)).thenReturn(Optional.of(documentoAjeno));

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarConSesionAcotadaADocumentoDistintoLanzaResourceNotFound() {
        sesion.setDocumentScopeRequired(true);
        Documento otroDocumento = new Documento();
        otroDocumento.setDocumentId(99L);
        sesion.setDocumento(otroDocumento);
        when(documentoRepository.findOwnedForProcessing(10L, 1L)).thenReturn(Optional.of(documento));

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarConChunkInexistenteLanzaResourceNotFound() {
        when(documentoRepository.findOwnedForProcessing(10L, 1L)).thenReturn(Optional.of(documento));
        when(chunkRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarConChunkDeOtroDocumentoLanzaResourceNotFound() {
        Documento otroDocumento = new Documento();
        otroDocumento.setDocumentId(11L);
        when(documentoRepository.findOwnedForProcessing(10L, 1L)).thenReturn(Optional.of(documento));
        when(chunkRepository.findById(100L)).thenReturn(Optional.of(chunk(otroDocumento)));

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.1))));
    }

    @Test
    void registrarExitosoDeduplicaChunksYCalculaRelevanceScore() {
        when(documentoRepository.findOwnedForProcessing(10L, 1L)).thenReturn(Optional.of(documento));
        when(chunkRepository.findById(100L)).thenReturn(Optional.of(chunk(documento)));

        service.registrar(respuestaAsistente, List.of(fuente(10L, 100L, 0.25), fuente(10L, 100L, 0.9)));

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(citacionesRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        var guardada = (CitacionesIA) captor.getValue().get(0);
        assertEquals(0.75, guardada.getRelevanceScore(), 0.0001);
        assertEquals(1, respuestaAsistente.getCitaciones().size());
    }

    @Test
    void listarPorMensajeConMensajeInexistenteLanzaResourceNotFound() {
        when(mensajesRepository.findByMessageIdAndSesionIdAndSenderType(30L, 20L, SenderType.ASSISTANT))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listarPorMensaje(20L, 30L));
    }

    @Test
    void listarPorMensajeDevuelveLasCitacionesMapeadas() {
        when(mensajesRepository.findByMessageIdAndSesionIdAndSenderType(30L, 20L, SenderType.ASSISTANT))
                .thenReturn(Optional.of(respuestaAsistente));
        CitacionesIA citacion = new CitacionesIA();
        citacion.setCitationId(1L);
        citacion.setDocumento(documento);
        citacion.setChunkId(100L);
        citacion.setRelevanceScore(0.8);
        when(citacionesRepository.findByMensajeMessageIdOrderByCitationIdAsc(30L)).thenReturn(List.of(citacion));

        var result = service.listarPorMensaje(20L, 30L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).documentId());
        assertEquals(0.8, result.get(0).relevanceScore());
    }
}
