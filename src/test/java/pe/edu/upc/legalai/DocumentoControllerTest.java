package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.DocumentoController;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoDownloadService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoPreparationService;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoProcessingService;
import pe.edu.upc.legalai.servicesinterfaces.IDocumentoService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private DocumentoResponseDTO sampleDocumento(Long id, EstadoProcesamiento status) {
        return new DocumentoResponseDTO(id, 5L, "contrato.pdf", "application/pdf", "/uploads/contrato.pdf",
                "CONTRACT", 1024L, status, LocalDateTime.now(), LocalDateTime.now());
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
                .andExpect(jsonPath("$[0].caseId").value(5));
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
}
