package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.entities.DocumentChunk;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ChunkGenerationException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentChunkRepository;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesimplements.CharacterChunker;
import pe.edu.upc.legalai.servicesimplements.DocumentChunkServiceImpl;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DocumentChunkServiceImplTest {

    private IDocumentoRepository documentoRepository;
    private IDocumentChunkRepository chunkRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private CharacterChunker chunker;
    private DocumentChunkServiceImpl service;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        documentoRepository = mock(IDocumentoRepository.class);
        chunkRepository = mock(IDocumentChunkRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        chunker = mock(CharacterChunker.class);
        service = new DocumentChunkServiceImpl(documentoRepository, chunkRepository, usuarioService,
                auditLogService, chunker);

        usuario = new Usuario();
        usuario.setUserId(1L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
    }

    private Documento documento(EstadoProcesamiento status, String texto) {
        Documento documento = new Documento();
        documento.setDocumentId(5L);
        documento.setProcessingStatus(status);
        documento.setExtractedText(texto);
        return documento;
    }

    @Test
    void generarConDocumentoInexistenteLanzaResourceNotFound() {
        when(documentoRepository.findOwnedForProcessing(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generar(5L));
        verifyNoInteractions(chunkRepository);
    }

    @Test
    void generarConDocumentoNoProcesadoLanzaBadRequest() {
        when(documentoRepository.findOwnedForProcessing(5L, 1L))
                .thenReturn(Optional.of(documento(EstadoProcesamiento.UPLOADED, null)));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.generar(5L));
        assertTrue(ex.getMessage().contains("PROCESSED"));
    }

    @Test
    void generarSinTextoExtraidoLanzaBadRequest() {
        when(documentoRepository.findOwnedForProcessing(5L, 1L))
                .thenReturn(Optional.of(documento(EstadoProcesamiento.PROCESSED, "   ")));

        assertThrows(BadRequestException.class, () -> service.generar(5L));
    }

    @Test
    void generarConFragmentosVaciosLanzaBadRequest() {
        when(documentoRepository.findOwnedForProcessing(5L, 1L))
                .thenReturn(Optional.of(documento(EstadoProcesamiento.PROCESSED, "texto util")));
        when(chunker.split("texto util")).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> service.generar(5L));
        verify(chunkRepository, never()).saveAll(any());
    }

    @Test
    void generarExitosoBorraLosChunksPreviosYGuardaLosNuevos() {
        when(documentoRepository.findOwnedForProcessing(5L, 1L))
                .thenReturn(Optional.of(documento(EstadoProcesamiento.PROCESSED, "texto util")));
        when(chunker.split("texto util")).thenReturn(List.of(
                new CharacterChunker.Fragment(0, 5, "texto"),
                new CharacterChunker.Fragment(5, 10, " util")));
        when(chunker.getChunkSize()).thenReturn(2500);
        when(chunker.getOverlap()).thenReturn(300);

        var response = service.generar(5L);

        assertEquals(5L, response.documentId());
        assertEquals(2, response.chunksCreated());
        assertEquals(2500, response.chunkSize());
        assertEquals(300, response.overlap());

        var inOrder = inOrder(chunkRepository);
        inOrder.verify(chunkRepository).deleteByDocumentoDocumentId(5L);
        inOrder.verify(chunkRepository).saveAll(any());
        verify(auditLogService).registrar(eq(usuario), eq("GENERATE_DOCUMENT_CHUNKS"), eq("Documento"), eq(5L), any());
    }

    @Test
    void generarConFalloDeRepositorioEnvuelveEnChunkGenerationException() {
        when(documentoRepository.findOwnedForProcessing(5L, 1L))
                .thenReturn(Optional.of(documento(EstadoProcesamiento.PROCESSED, "texto util")));
        when(chunker.split("texto util")).thenReturn(List.of(new CharacterChunker.Fragment(0, 5, "texto")));
        when(chunkRepository.saveAll(any())).thenThrow(new RuntimeException("fallo bd"));

        assertThrows(ChunkGenerationException.class, () -> service.generar(5L));
    }

    @Test
    void listarConDocumentoAjenoLanzaResourceNotFound() {
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listar(5L));
        verifyNoInteractions(chunkRepository);
    }

    @Test
    void listarDevuelveLosChunksOrdenadosPorIndice() {
        when(documentoRepository.findByDocumentIdAndExpedienteOwnerUserId(5L, 1L))
                .thenReturn(Optional.of(documento(EstadoProcesamiento.PROCESSED, "texto")));
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkIndex(0);
        chunk.setContent("texto");
        chunk.setCharStart(0);
        chunk.setCharEnd(5);
        when(chunkRepository.findByDocumentoDocumentIdOrderByChunkIndexAsc(5L)).thenReturn(List.of(chunk));

        var result = service.listar(5L);

        assertEquals(1, result.size());
        assertEquals("texto", result.get(0).content());
        assertEquals(5L, result.get(0).documentId());
    }
}
