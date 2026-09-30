package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import pe.edu.upc.legalai.entities.*;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.repositories.*;
import pe.edu.upc.legalai.servicesimplements.DocumentoServiceImpl;
import pe.edu.upc.legalai.servicesinterfaces.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoUploadTest {
    @TempDir Path temp;
    private final DocumentoRepository documents = mock(DocumentoRepository.class);
    private final ExpedienteRepository cases = mock(ExpedienteRepository.class);
    private final UsuarioService users = mock(UsuarioService.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final Usuario user = new Usuario();
    private final Expediente expediente = new Expediente();
    private DocumentoServiceImpl service;
    private static final byte[] PDF = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    @BeforeEach
    void setup() {
        user.setUserId(7L);
        expediente.setCaseId(12L);
        expediente.setOwner(user);
        when(users.obtenerUsuarioAutenticado()).thenReturn(user);
        when(cases.findByCaseIdAndOwnerUserId(12L, 7L)).thenReturn(Optional.of(expediente));
        when(documents.saveAndFlush(any())).thenAnswer(invocation -> {
            Documento doc = invocation.getArgument(0);
            doc.setDocumentId(25L);
            return doc;
        });
        service = createService(temp.resolve("uploads"));
    }

    private DocumentoServiceImpl createService(Path root) {
        return new DocumentoServiceImpl(documents, cases, users, audit, root.toString(), DataSize.ofBytes(1024));
    }

    private MockMultipartFile pdf() { return file("contrato.pdf", "application/pdf", PDF); }
    private MockMultipartFile file(String name, String mime, byte[] bytes) {
        return new MockMultipartFile("file", name, mime, bytes);
    }

    @Test
    void storesPdfMetadataAuditAndUniqueInternalReference() throws Exception {
        var response = service.subirArchivo(12L, pdf(), "Contrato");
        assertThat(response.getStorageUrl()).matches("[a-f0-9-]{36}\\.pdf");
        assertThat(response.getFileName()).isEqualTo("contrato.pdf");
        assertThat(response.getFileType()).isEqualTo("application/pdf");
        assertThat(response.getSizeBytes()).isEqualTo(PDF.length);
        assertThat(response.getCategory()).isEqualTo("Contrato");
        assertThat(response.getCaseId()).isEqualTo(12L);
        assertThat(response.getProcessingStatus()).isEqualTo(EstadoProcesamiento.UPLOADED);
        assertThat(Files.readAllBytes(temp.resolve("uploads").resolve(response.getStorageUrl()))).isEqualTo(PDF);
        var captor = org.mockito.ArgumentCaptor.forClass(Documento.class);
        verify(documents).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUploadedBy()).isSameAs(user);
        assertThat(captor.getValue().getExpediente()).isSameAs(expediente);
        verify(audit).registrar(user, "UPLOAD_DOCUMENT", "Documento", 25L, "caseId=12; fileName=contrato.pdf");
        var second = service.subirArchivo(12L, pdf(), null);
        assertThat(second.getStorageUrl()).isNotEqualTo(response.getStorageUrl());
        assertThat(second.getCategory()).isNull();
    }

    @Test
    void rejectsMissingEmptyWrongMimeExtensionAndSignature() {
        var invalid = java.util.Arrays.asList(null, file("x.pdf", "application/pdf", new byte[0]),
                file("x.pdf", "text/plain", PDF), file("x.txt", "application/pdf", PDF),
                file("x.pdf", "application/pdf", "not-a-pdf".getBytes(StandardCharsets.UTF_8)));
        for (var value : invalid) {
            assertThatThrownBy(() -> service.subirArchivo(12L, value, null)).isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(documents, audit);
        assertThat(temp.resolve("uploads")).doesNotExist();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "../../x.pdf", "..\\x.pdf", "C:\\x.pdf", "bad\nname.pdf", "x:y.pdf"})
    void rejectsUnsafeNames(String name) {
        assertThatThrownBy(() -> service.subirArchivo(12L, file(name, "application/pdf", PDF), null))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(documents, audit);
    }

    @Test
    void rejectsOversizeAndLongCategory() {
        assertThatThrownBy(() -> service.subirArchivo(12L, file("x.pdf", "application/pdf", new byte[1025]), null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.subirArchivo(12L, pdf(), "x".repeat(81))).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(documents, audit);
    }

    @ParameterizedTest
    @ValueSource(longs = {13L, 999L})
    void rejectsForeignOrMissingCaseBeforeReadingFile(long id) {
        // Repository scoped by owner returns empty for both a foreign ID and an absent ID.
        when(cases.findByCaseIdAndOwnerUserId(id, 7L)).thenReturn(Optional.empty());
        var unread = mock(org.springframework.web.multipart.MultipartFile.class);
        assertThatThrownBy(() -> service.subirArchivo(id, unread, null)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(unread, documents, audit);
        assertThat(temp.resolve("uploads")).doesNotExist();
    }

    @Test
    void cleansFileWhenPersistenceOrAuditFails() throws Exception {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("private")).when(documents).saveAndFlush(any());
        assertThatThrownBy(() -> service.subirArchivo(12L, pdf(), null)).isInstanceOf(DocumentUploadException.class);
        assertStorageEmpty();
        verifyNoInteractions(audit);
        reset(documents);
        when(documents.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new IllegalStateException("audit failed")).when(audit).registrar(any(), any(), any(), any(), any());
        assertThatThrownBy(() -> service.subirArchivo(12L, pdf(), null)).isInstanceOf(DocumentUploadException.class);
        assertStorageEmpty();
    }

    @Test
    void rollbackAfterMethodReturnsRemovesFileButCommitRetainsIt() throws Exception {
        for (int status : new int[]{TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK}) {
            TransactionSynchronizationManager.initSynchronization();
            try {
                var response = service.subirArchivo(12L, pdf(), null);
                Path stored = temp.resolve("uploads").resolve(response.getStorageUrl());
                assertThat(stored).exists();
                for (var synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                    synchronization.afterCompletion(status);
                }
                assertThat(Files.exists(stored)).isEqualTo(status == TransactionSynchronization.STATUS_COMMITTED);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Test
    void unavailableStorageDoesNotCreateMetadata() throws Exception {
        Path blocked = Files.writeString(temp.resolve("blocked"), "not a directory");
        service = createService(blocked);
        assertThatThrownBy(() -> service.subirArchivo(12L, pdf(), null)).isInstanceOf(DocumentUploadException.class);
        verifyNoInteractions(documents, audit);
        assertThat(Files.readString(blocked)).isEqualTo("not a directory");
    }

    @Test
    void partialWriteFailureCleansFile() throws Exception {
        var broken = new MockMultipartFile("file", "broken.pdf", "application/pdf", PDF) {
            @Override public InputStream getInputStream() {
                return new InputStream() {
                    int position;
                    @Override public int read() throws IOException {
                        if (position == 10) throw new IOException("read failed");
                        return PDF[position++];
                    }
                };
            }
        };
        assertThatThrownBy(() -> service.subirArchivo(12L, broken, null)).isInstanceOf(DocumentUploadException.class);
        assertStorageEmpty();
        verifyNoInteractions(documents, audit);
    }

    private void assertStorageEmpty() throws IOException {
        try (var files = Files.list(temp.resolve("uploads"))) { assertThat(files).isEmpty(); }
    }
}
