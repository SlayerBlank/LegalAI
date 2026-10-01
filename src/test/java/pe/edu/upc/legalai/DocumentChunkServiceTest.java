package pe.edu.upc.legalai;

import org.junit.jupiter.api.*;
import pe.edu.upc.legalai.entities.*;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.repositories.*;
import pe.edu.upc.legalai.servicesinterfaces.*;
import pe.edu.upc.legalai.servicesimplements.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentChunkServiceTest {
    final IDocumentoRepository documents = mock(IDocumentoRepository.class);
    final IDocumentChunkRepository chunks = mock(IDocumentChunkRepository.class);
    final UsuarioService users = mock(UsuarioService.class);
    final AuditLogService audit = mock(AuditLogService.class);
    final Documento document = new Documento();
    DocumentChunkService service;

    @BeforeEach void setup() {
        Usuario user = new Usuario(); user.setUserId(7L);
        when(users.obtenerUsuarioAutenticado()).thenReturn(user);
        document.setDocumentId(1L); document.setProcessingStatus(EstadoProcesamiento.PROCESSED);
        document.setExtractedText("Texto legal. ".repeat(700));
        when(documents.findOwnedForProcessing(1L, 7L)).thenReturn(Optional.of(document));
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(1L, 7L)).thenReturn(Optional.of(document));
        service = new DocumentChunkServiceImpl(documents, chunks, users, audit, new CharacterChunker(2500,300));
    }

    @Test void sequentialIndicesBulkSaveAndAuditWithoutContent() {
        var response = service.generar(1L);
        assertThat(response.chunksCreated()).isGreaterThan(1);
        assertThat(response.chunkSize()).isEqualTo(2500); assertThat(response.overlap()).isEqualTo(300);
        var order = inOrder(chunks);
        order.verify(chunks).deleteByDocumentoDocumentId(1L);
        order.verify(chunks).saveAll(argThat(list -> {
            int index = 0;
            for (var chunk : list) {
                assertThat(chunk.getChunkIndex()).isEqualTo(index++);
                assertThat(chunk.getDocumento()).isSameAs(document);
                assertThat(chunk.getCharStart()).isLessThan(chunk.getCharEnd());
                assertThat(chunk.getContent()).isNotBlank();
            }
            return index == response.chunksCreated();
        }));
        assertThat(document.getProcessingStatus()).isEqualTo(EstadoProcesamiento.PROCESSED);
        verify(audit).registrar(any(),eq("GENERATE_DOCUMENT_CHUNKS"),eq("Documento"),eq(1L),eq("chunksCreated="+response.chunksCreated()));
    }

    @Test void missingAndForeignDocumentsAreRejected() {
        for (long id : new long[]{2,3}) {
            assertThatThrownBy(() -> service.generar(id)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.listar(id)).isInstanceOf(ResourceNotFoundException.class);
        }
        verifyNoInteractions(chunks, audit);
    }

    @Test void rejectsNullEmptyAndUnprocessedWithoutDeleting() {
        for (String text : Arrays.asList(null,""," \r\n\t")) {
            document.setExtractedText(text);
            assertThatThrownBy(() -> service.generar(1L)).isInstanceOf(BadRequestException.class);
        }
        document.setExtractedText("Texto");
        for (var state : List.of(EstadoProcesamiento.UPLOADED,EstadoProcesamiento.PROCESSING,EstadoProcesamiento.ERROR)) {
            document.setProcessingStatus(state);
            assertThatThrownBy(() -> service.generar(1L)).isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(chunks,audit);
    }

    @Test void wrapsPersistenceFailureAndRetainsCause() {
        var failure = new org.springframework.dao.DataIntegrityViolationException("simulated constraint");
        doThrow(failure).when(chunks).saveAll(any());
        assertThatThrownBy(() -> service.generar(1L)).isInstanceOf(ChunkGenerationException.class).hasCause(failure);
        verifyNoInteractions(audit);
    }
}
