package pe.edu.upc.legalai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentChunkRepository;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.servicesimplements.DocumentStorage;
import pe.edu.upc.legalai.servicesimplements.DocumentoProcessingServiceImpl;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoProcessingService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU-028 (texto extraido) y HU-058 (procesar PDF). */
class DocumentoProcessingServiceImplTest {

    private IDocumentoRepository documents;
    private IDocumentChunkRepository chunks;
    private AuditLogService audit;
    private IDocumentoService metadata;
    private DocumentStorage storage;
    private DocumentoProcessingServiceImpl service;
    private Path pdf;

    @BeforeEach
    void setup() {
        documents = mock(IDocumentoRepository.class);
        chunks = mock(IDocumentChunkRepository.class);
        audit = mock(AuditLogService.class);
        metadata = mock(IDocumentoService.class);
        storage = mock(DocumentStorage.class);
        IUsuarioService users = mock(IUsuarioService.class);
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);
        when(users.obtenerUsuarioAutenticado()).thenReturn(usuario);
        // Con un PlatformTransactionManager simulado, TransactionTemplate ejecuta el bloque sin BD real.
        service = new DocumentoProcessingServiceImpl(documents, users, audit, metadata,
                mock(PlatformTransactionManager.class), storage, chunks);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (pdf != null) Files.deleteIfExists(pdf);
    }

    private Documento documento(Long id, EstadoProcesamiento status, String text) {
        Documento documento = new Documento();
        documento.setDocumentId(id);
        documento.setStorageUrl("doc-" + id + ".pdf");
        documento.setProcessingStatus(status);
        documento.setExtractedText(text);
        return documento;
    }

    private Path crearPdf(String texto) throws Exception {
        pdf = Files.createTempFile("legalai-proc", ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (texto != null) {
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(texto);
                    content.endText();
                }
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }

    // ---------- HU-028

    @Test
    void obtenerTextoDeDocumentoProcesadoDevuelveElTexto() {
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L))
                .thenReturn(Optional.of(documento(7L, EstadoProcesamiento.PROCESSED, "Contrato de alquiler")));

        var result = service.obtenerTexto(7L);

        assertEquals("Contrato de alquiler", result.text());
        assertEquals(EstadoProcesamiento.PROCESSED, result.processingStatus());
        assertNull(result.message());
    }

    @Test
    void obtenerTextoDeDocumentoSinProcesarInformaElEstado() {
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L))
                .thenReturn(Optional.of(documento(7L, EstadoProcesamiento.UPLOADED, null)));

        var result = service.obtenerTexto(7L);

        assertNull(result.text());
        assertEquals(EstadoProcesamiento.UPLOADED, result.processingStatus());
        assertTrue(result.message().contains("aun no ha sido procesado"));
    }

    @Test
    void obtenerTextoDeDocumentoConErrorInformaQueSePuedeReintentar() {
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L))
                .thenReturn(Optional.of(documento(7L, EstadoProcesamiento.ERROR, null)));

        assertTrue(service.obtenerTexto(7L).message().contains("reintentar"));
    }

    @Test
    void obtenerTextoDeDocumentoAjenoLanzaResourceNotFound() {
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTexto(8L));
    }

    // ---------- HU-058

    @Test
    void procesarExtraeYGuardaElTextoDelPdf() throws Exception {
        Documento documento = documento(7L, EstadoProcesamiento.UPLOADED, null);
        when(documents.findOwnedForProcessing(7L, 1L)).thenReturn(Optional.of(documento));
        when(storage.resolve("doc-7.pdf")).thenReturn(crearPdf("Contrato de alquiler"));

        service.procesar(7L);

        assertEquals(EstadoProcesamiento.PROCESSED, documento.getProcessingStatus());
        assertTrue(documento.getExtractedText().contains("Contrato de alquiler"));
        verify(chunks).deleteByDocumentoDocumentId(7L);
        verify(audit).registrar(any(), eq("PROCESS_DOCUMENT"), eq("Documento"), eq(7L), any());
        verify(metadata).buscarPorId(7L);
    }

    @Test
    void procesarPdfSinTextoMarcaErrorYLanzaBadRequest() throws Exception {
        Documento documento = documento(7L, EstadoProcesamiento.UPLOADED, null);
        when(documents.findOwnedForProcessing(7L, 1L)).thenReturn(Optional.of(documento));
        when(storage.resolve("doc-7.pdf")).thenReturn(crearPdf(null));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.procesar(7L));

        assertTrue(ex.getMessage().contains("escaneado"));
        assertEquals(EstadoProcesamiento.ERROR, documento.getProcessingStatus());
        assertNull(documento.getExtractedText());
    }

    @Test
    void procesarDocumentoAjenoLanzaResourceNotFound() {
        when(documents.findOwnedForProcessing(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.procesar(8L));
        verify(documents, never()).saveAndFlush(any());
    }

    @Test
    void procesarDocumentoQueSeEstaProcesandoLanzaConflicto() {
        Documento documento = documento(7L, EstadoProcesamiento.PROCESSING, null);
        ReflectionTestUtils.setField(documento, "updatedAt", LocalDateTime.now().minusMinutes(1));
        when(documents.findOwnedForProcessing(7L, 1L)).thenReturn(Optional.of(documento));

        assertThrows(DuplicateResourceException.class, () -> service.procesar(7L));
        verify(documents, never()).saveAndFlush(any());
    }

    @Test
    void procesarDocumentoTrabadoEnProcessingPermiteReintentar() throws Exception {
        Documento documento = documento(7L, EstadoProcesamiento.PROCESSING, null);
        ReflectionTestUtils.setField(documento, "updatedAt", LocalDateTime.now().minusMinutes(30));
        when(documents.findOwnedForProcessing(7L, 1L)).thenReturn(Optional.of(documento));
        when(storage.resolve("doc-7.pdf")).thenReturn(crearPdf("Texto recuperado"));

        service.procesar(7L);

        assertEquals(EstadoProcesamiento.PROCESSED, documento.getProcessingStatus());
    }

    @Test
    void procesamientoEnCursoSoloSiEstaEnProcessingYEsReciente() {
        LocalDateTime ahora = LocalDateTime.now();
        assertTrue(DocumentoProcessingService.procesamientoEnCurso(EstadoProcesamiento.PROCESSING, ahora.minusMinutes(5)));
        assertFalse(DocumentoProcessingService.procesamientoEnCurso(EstadoProcesamiento.PROCESSING, ahora.minusMinutes(11)));
        assertFalse(DocumentoProcessingService.procesamientoEnCurso(EstadoProcesamiento.UPLOADED, ahora));
    }
}
