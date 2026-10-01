package pe.edu.upc.legalai;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.*;
import pe.edu.upc.legalai.entities.*;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.servicesimplements.DocumentoProcessingServiceImpl;
import pe.edu.upc.legalai.servicesinterfaces.*;

import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoProcessingTest {
    @TempDir Path storage;
    final IDocumentoRepository documents = mock(IDocumentoRepository.class);
    final UsuarioService users = mock(UsuarioService.class);
    final AuditLogService audit = mock(AuditLogService.class);
    final DocumentoService metadata = mock(DocumentoService.class);
    final Documento document = new Documento();
    final List<EstadoProcesamiento> states = new ArrayList<>();
    DocumentoProcessingServiceImpl service;

    @BeforeEach void setup() {
        Usuario user = new Usuario(); user.setUserId(7L);
        when(users.obtenerUsuarioAutenticado()).thenReturn(user);
        document.setDocumentId(1L); document.setStorageUrl("test.pdf");
        document.setProcessingStatus(EstadoProcesamiento.UPLOADED);
        when(documents.findOwnedForProcessing(1L, 7L)).thenReturn(Optional.of(document));
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(1L, 7L)).thenReturn(Optional.of(document));
        when(documents.saveAndFlush(document)).thenAnswer(i -> { states.add(document.getProcessingStatus()); return document; });
        var manager = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition definition) { }
            protected void doCommit(DefaultTransactionStatus status) { }
            protected void doRollback(DefaultTransactionStatus status) { }
        };
        service = new DocumentoProcessingServiceImpl(documents, users, audit, metadata, manager, storage.toString(),
                mock(pe.edu.upc.legalai.repositories.IDocumentChunkRepository.class));
    }

    static void pdf(Path path, String text) throws Exception {
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page);
            if (text != null) {
                try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(40, 700); content.showText(text); content.endText();
                }
            }
            pdf.save(path.toFile());
        }
    }

    @ParameterizedTest @ValueSource(strings={"test.pdf", "uploads/test.pdf"})
    void extractsTextAndTransitionsAndAudits(String reference) throws Exception {
        pdf(storage.resolve("test.pdf"), "Contrato de prueba LegalAI");
        document.setStorageUrl(reference);
        service.procesar(1L);
        assertThat(states).containsExactly(EstadoProcesamiento.PROCESSING, EstadoProcesamiento.PROCESSED);
        assertThat(document.getExtractedText()).isEqualTo("Contrato de prueba LegalAI");
        verify(audit).registrar(any(), eq("PROCESS_DOCUMENT"), eq("Documento"), eq(1L), eq("extractedTextLength=26"));
        assertThat(service.obtenerTexto(1L).text()).isEqualTo(document.getExtractedText());
    }

    @Test void missingAndForeignDocumentsDoNotReadOrWrite() {
        for (long id : new long[]{2, 3}) {
            assertThatThrownBy(() -> service.procesar(id)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.obtenerTexto(id)).isInstanceOf(ResourceNotFoundException.class);
        }
        verify(documents, never()).saveAndFlush(any()); verifyNoInteractions(audit, metadata);
    }

    @Test void missingFileMarksError() {
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(DocumentProcessingException.class)
                .hasCauseInstanceOf(java.nio.file.NoSuchFileException.class);
        assertError();
    }

    @Test void corruptPdfMarksError() throws Exception {
        Files.writeString(storage.resolve("test.pdf"), "%PDF-1.7 corrupt");
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(DocumentProcessingException.class);
        assertError();
    }

    @Test void emptyPdfMarksErrorWithClearMessage() throws Exception {
        pdf(storage.resolve("test.pdf"), null);
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(BadRequestException.class)
                .hasMessage("No se pudo extraer texto del PDF. El documento puede ser escaneado.");
        assertError();
    }

    @ParameterizedTest @ValueSource(strings={"../outside.pdf", "uploads/../outside.pdf", "/outside.pdf", "C:\\outside.pdf", "other/test.pdf"})
    void unsafeReferencesMarkError(String reference) {
        document.setStorageUrl(reference);
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(DocumentProcessingException.class);
        assertError();
    }

    @Test void activeProcessingIsRejectedWithoutChangingState() {
        document.setProcessingStatus(EstadoProcesamiento.PROCESSING);
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(DuplicateResourceException.class);
        assertThat(states).isEmpty(); verifyNoInteractions(audit);
    }

    @Test void databaseFailureIsLoggedAndErrorIsPersisted() throws Exception {
        pdf(storage.resolve("test.pdf"), "Texto");
        doAnswer(i -> {
            states.add(document.getProcessingStatus());
            if (document.getProcessingStatus() == EstadoProcesamiento.PROCESSED)
                throw new org.springframework.dao.DataIntegrityViolationException("simulated SQL failure");
            return document;
        }).when(documents).saveAndFlush(document);
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(DocumentProcessingException.class);
        assertThat(document.getProcessingStatus()).isEqualTo(EstadoProcesamiento.ERROR);
        assertThat(document.getExtractedText()).isNull();
        assertThat(states).containsExactly(EstadoProcesamiento.PROCESSING, EstadoProcesamiento.PROCESSED, EstadoProcesamiento.ERROR);
        verifyNoInteractions(audit);
    }

    @Test void auditFailureMarksError() throws Exception {
        pdf(storage.resolve("test.pdf"), "Texto");
        doThrow(new IllegalStateException("audit failed")).when(audit).registrar(any(), any(), any(), any(), any());
        assertThatThrownBy(() -> service.procesar(1L)).isInstanceOf(DocumentProcessingException.class);
        assertThat(document.getProcessingStatus()).isEqualTo(EstadoProcesamiento.ERROR);
        assertThat(document.getExtractedText()).isNull();
    }

    void assertError() {
        assertThat(states).containsExactly(EstadoProcesamiento.PROCESSING, EstadoProcesamiento.ERROR);
        assertThat(document.getExtractedText()).isNull(); verifyNoInteractions(audit, metadata);
    }
}
