package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.EmbeddingException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.ChunkEmbeddingRepository;
import pe.edu.upc.legalai.repositories.IDocumentChunkRepository;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.servicesimplements.DocumentoPreparationServiceImpl;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentChunkService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoProcessingService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU-029: preparar un documento para analisis con IA. */
class DocumentoPreparationServiceImplTest {

    private IDocumentoService documents;
    private DocumentoProcessingService processing;
    private DocumentChunkService chunking;
    private SemanticSearchService semantic;
    private IDocumentChunkRepository chunks;
    private ChunkEmbeddingRepository vectors;
    private AuditLogService audit;
    private IDocumentoRepository documentRows;
    private DocumentoPreparationServiceImpl service;

    @BeforeEach
    void setup() {
        documents = mock(IDocumentoService.class);
        processing = mock(DocumentoProcessingService.class);
        chunking = mock(DocumentChunkService.class);
        semantic = mock(SemanticSearchService.class);
        chunks = mock(IDocumentChunkRepository.class);
        vectors = mock(ChunkEmbeddingRepository.class);
        audit = mock(AuditLogService.class);
        documentRows = mock(IDocumentoRepository.class);
        IUsuarioService users = mock(IUsuarioService.class);
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);
        when(users.obtenerUsuarioAutenticado()).thenReturn(usuario);
        service = new DocumentoPreparationServiceImpl(documents, processing, chunking, semantic, chunks, vectors,
                users, audit, mock(PlatformTransactionManager.class), documentRows);
    }

    private DocumentoResponseDTO dto(EstadoProcesamiento status, int textLength, LocalDateTime updatedAt) {
        DocumentoResponseDTO dto = new DocumentoResponseDTO(7L, 5L, "a.pdf", "application/pdf", "a.pdf", null,
                10L, status, LocalDateTime.now(), updatedAt);
        dto.setExtractedTextLength(textLength);
        return dto;
    }

    @Test
    void prepararDocumentoProcesadoGeneraFragmentosYEmbeddings() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.PROCESSED, 100, LocalDateTime.now()));
        when(chunks.countByDocumentoDocumentId(7L)).thenReturn(0L, 3L);
        when(vectors.countValidChunks(7L)).thenReturn(3L);

        var result = service.preparar(7L);

        verify(processing, never()).procesar(anyLong());
        verify(chunking).generar(7L);
        verify(semantic).generar(7L, false);
        assertEquals("READY", result.stage());
        assertTrue(result.readyForRag());
    }

    @Test
    void prepararDocumentoSinTextoLoProcesaPrimero() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.UPLOADED, 0, LocalDateTime.now()));
        when(chunks.countByDocumentoDocumentId(7L)).thenReturn(0L);

        service.preparar(7L);

        verify(processing).procesar(7L);
    }

    @Test
    void siFallanLosEmbeddingsElDocumentoQuedaEnErrorYSeAudita() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.PROCESSED, 100, LocalDateTime.now()));
        when(chunks.countByDocumentoDocumentId(7L)).thenReturn(3L);
        when(semantic.generar(anyLong(), anyBoolean())).thenThrow(new EmbeddingException("Servicio no disponible"));
        Documento fila = new Documento();
        fila.setProcessingStatus(EstadoProcesamiento.PROCESSED);
        when(documentRows.findById(7L)).thenReturn(Optional.of(fila));

        assertThrows(EmbeddingException.class, () -> service.preparar(7L));

        assertEquals(EstadoProcesamiento.ERROR, fila.getProcessingStatus());
        verify(documentRows).saveAndFlush(fila);
        verify(audit).registrar(any(), eq("PREPARE_DOCUMENT"), eq("Documento"), eq(7L), contains("stage=EMBEDDINGS; result=FAILED"));
    }

    @Test
    void siFallaLaExtraccionNoSeMarcaErrorDesdeLaPreparacion() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.UPLOADED, 0, LocalDateTime.now()));
        when(processing.procesar(7L)).thenThrow(new RuntimeException("PDF invalido"));

        assertThrows(RuntimeException.class, () -> service.preparar(7L));

        // DocumentoProcessingServiceImpl ya marca ERROR en su propia transaccion.
        verify(documentRows, never()).saveAndFlush(any());
    }

    @Test
    void prepararDocumentoQueSeEstaProcesandoLanzaConflicto() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.PROCESSING, 0, LocalDateTime.now()));

        assertThrows(DuplicateResourceException.class, () -> service.preparar(7L));
        verify(processing, never()).procesar(anyLong());
    }

    @Test
    void prepararDocumentoAjenoLanzaResourceNotFound() {
        when(documents.buscarPorId(8L)).thenThrow(new ResourceNotFoundException("Documento no encontrado"));

        assertThrows(ResourceNotFoundException.class, () -> service.preparar(8L));
        verifyNoInteractions(processing, chunking, semantic, audit);
    }

    @Test
    void estadoDeDocumentoEnErrorConTextoNoEstaListo() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.ERROR, 100, LocalDateTime.now()));
        when(chunks.countByDocumentoDocumentId(7L)).thenReturn(3L);
        when(vectors.countValidChunks(7L)).thenReturn(3L);

        var result = service.estado(7L);

        assertEquals("EMBEDDINGS_REQUIRED", result.stage());
        assertFalse(result.readyForRag());
    }

    @Test
    void estadoDeDocumentoEnErrorSinTextoEsErrorDeExtraccion() {
        when(documents.buscarPorId(7L)).thenReturn(dto(EstadoProcesamiento.ERROR, 0, LocalDateTime.now()));

        assertEquals("EXTRACTION_ERROR", service.estado(7L).stage());
    }
}
