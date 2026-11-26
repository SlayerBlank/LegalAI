package pe.edu.upc.legalai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.servicesimplements.DocumentStorage;
import pe.edu.upc.legalai.servicesimplements.DocumentoDownloadServiceImpl;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** HU-027: descargar el PDF original. */
class DocumentoDownloadServiceImplTest {

    private IDocumentoRepository documents;
    private DocumentStorage storage;
    private DocumentoDownloadServiceImpl service;
    private Path pdf;

    @BeforeEach
    void setup() {
        documents = mock(IDocumentoRepository.class);
        storage = mock(DocumentStorage.class);
        IUsuarioService users = mock(IUsuarioService.class);
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);
        when(users.obtenerUsuarioAutenticado()).thenReturn(usuario);
        service = new DocumentoDownloadServiceImpl(documents, users, storage);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (pdf != null) Files.deleteIfExists(pdf);
    }

    private Documento documento(String fileName) {
        Documento documento = new Documento();
        documento.setDocumentId(7L);
        documento.setFileName(fileName);
        documento.setStorageUrl("uuid.pdf");
        return documento;
    }

    @Test
    void descargarDevuelveElArchivoOriginalConSuNombreYTamano() throws Exception {
        byte[] contenido = "%PDF-1.7 contenido".getBytes();
        pdf = Files.write(Files.createTempFile("legalai-down", ".pdf"), contenido);
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L))
                .thenReturn(Optional.of(documento("contrato \"final\".pdf")));
        when(storage.resolve("uuid.pdf")).thenReturn(pdf);

        var download = service.descargar(7L);

        assertEquals("contrato _final_.pdf", download.fileName());
        assertEquals(contenido.length, download.sizeBytes());
        try (InputStream input = download.resource().getInputStream()) {
            assertArrayEquals(contenido, input.readAllBytes());
        }
    }

    @Test
    void descargarDocumentoAjenoLanzaResourceNotFound() {
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.descargar(8L));
        verifyNoInteractions(storage);
    }

    @Test
    void descargarDocumentoSinArchivoEnStorageLanzaResourceNotFound() throws Exception {
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L)).thenReturn(Optional.of(documento("a.pdf")));
        when(storage.resolve("uuid.pdf")).thenThrow(new IOException("Archivo PDF no disponible en storage"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.descargar(7L));
        assertEquals("Archivo del documento no disponible", ex.getMessage());
    }

    @Test
    void descargarDocumentoSinCargaVerificableLanzaResourceNotFound() {
        Documento documento = documento("a.pdf");
        when(documents.findByDocumentIdAndExpedienteOwnerUserId(7L, 1L)).thenReturn(Optional.of(documento));
        doThrow(new ResourceNotFoundException("El documento no tiene un archivo de carga verificable"))
                .when(storage).verifyUpload(documento);

        assertThrows(ResourceNotFoundException.class, () -> service.descargar(7L));
    }
}
