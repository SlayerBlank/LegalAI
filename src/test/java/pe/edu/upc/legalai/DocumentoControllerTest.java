package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.DocumentoController;
import pe.edu.upc.legalai.dtos.request.DocumentoUpdateRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoPendienteResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoTextResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoDownloadService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoProcessingService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentoControllerTest {

    private IDocumentoService documentoService;
    private DocumentoProcessingService processingService;
    private DocumentoDownloadService downloads;
    private DocumentoPreparationService preparation;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        documentoService = mock(IDocumentoService.class);
        processingService = mock(DocumentoProcessingService.class);
        downloads = mock(DocumentoDownloadService.class);
        preparation = mock(DocumentoPreparationService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new DocumentoController(documentoService, processingService, downloads, preparation))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private DocumentoPendienteResponseDTO sampleDocumento(Long id, EstadoProcesamiento status) {
        return new DocumentoPendienteResponseDTO(id, 5L, "Desalojo Lopez", "contrato.pdf", "application/pdf",
                "/uploads/contrato.pdf", "CONTRACT", 1024L, status, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listarPendientesRevisionSinFiltroDevuelveElListadoDelAbogadoIndicadoHU67() throws Exception {
        when(documentoService.listarPendientesRevision(eq(1L), isNull())).thenReturn(List.of(
                sampleDocumento(1L, EstadoProcesamiento.UPLOADED),
                sampleDocumento(2L, EstadoProcesamiento.ERROR)));

        mvc.perform(get("/api/documents/pending-review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").value("contrato.pdf"))
                .andExpect(jsonPath("$[0].caseId").value(5))
                .andExpect(jsonPath("$[0].caseTitle").value("Desalojo Lopez"));
    }

    @Test
    void listarPendientesRevisionConFiltroDeEstadoDelegaEseEstado() throws Exception {
        when(documentoService.listarPendientesRevision(eq(1L), eq(EstadoProcesamiento.PROCESSING)))
                .thenReturn(List.of(sampleDocumento(3L, EstadoProcesamiento.PROCESSING)));

        mvc.perform(get("/api/documents/pending-review/1").param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].processingStatus").value("PROCESSING"));
    }

    @Test
    void listarPendientesRevisionSinRegistrosDevuelve200YListaVacia() throws Exception {
        when(documentoService.listarPendientesRevision(eq(1L), isNull())).thenReturn(List.of());

        mvc.perform(get("/api/documents/pending-review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarPendientesRevisionConAbogadoInexistenteDevuelve404() throws Exception {
        when(documentoService.listarPendientesRevision(eq(99L), isNull()))
                .thenThrow(new ResourceNotFoundException("Abogado no encontrado"));

        mvc.perform(get("/api/documents/pending-review/99"))
                .andExpect(status().isNotFound());
    }

    // ---------- HU-025: actualizar metadatos

    @Test
    void actualizarCategoriaDevuelve200ConElDocumento() throws Exception {
        DocumentoResponseDTO actualizado = new DocumentoResponseDTO(7L, 5L, "contrato.pdf", "application/pdf",
                "a.pdf", "Contratos", 1024L, EstadoProcesamiento.PROCESSED, LocalDateTime.now(), LocalDateTime.now());
        when(documentoService.actualizar(eq(7L), any(DocumentoUpdateRequestDTO.class))).thenReturn(actualizado);

        mvc.perform(put("/api/documents/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Contratos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Contratos"));
    }

    @Test
    void actualizarConCategoriaMuyLargaDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(put("/api/documents/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"" + "x".repeat(81) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("category: La categoria no debe superar los 80 caracteres"));
        verify(documentoService, never()).actualizar(any(), any());
    }

    @Test
    void actualizarDocumentoAjenoDevuelve404() throws Exception {
        when(documentoService.actualizar(eq(8L), any(DocumentoUpdateRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Documento no encontrado"));

        mvc.perform(put("/api/documents/8").contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- HU-027: descarga

    @Test
    void descargarDevuelvePdfConContentDisposition() throws Exception {
        byte[] pdf = "%PDF-1.7 contenido".getBytes();
        when(downloads.descargar(7L)).thenReturn(
                new DocumentoDownloadService.Download(new ByteArrayResource(pdf), "contrato.pdf", pdf.length));

        mvc.perform(get("/api/documents/7/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("contrato.pdf")))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void descargarDocumentoSinArchivoDevuelve404() throws Exception {
        when(downloads.descargar(9L)).thenThrow(new ResourceNotFoundException("Archivo del documento no disponible"));

        mvc.perform(get("/api/documents/9/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Archivo del documento no disponible"));
    }

    // ---------- HU-028: texto

    @Test
    void textoDeDocumentoSinProcesarInformaSuEstado() throws Exception {
        when(processingService.obtenerTexto(7L)).thenReturn(new DocumentoTextResponseDTO(7L,
                EstadoProcesamiento.UPLOADED, null, "El documento aun no ha sido procesado."));

        mvc.perform(get("/api/documents/7/text"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("UPLOADED"))
                .andExpect(jsonPath("$.message").value("El documento aun no ha sido procesado."));
    }
}
