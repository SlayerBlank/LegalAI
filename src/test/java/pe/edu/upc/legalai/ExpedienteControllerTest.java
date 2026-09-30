package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.DTOs.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.DTOs.response.ExpedienteResponseDTO;
import pe.edu.upc.legalai.controllers.ExpedienteController;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoService;
import pe.edu.upc.legalai.servicesinterfaces.ExpedienteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExpedienteControllerTest {

    private ExpedienteService expedienteService;
    private DocumentoService documentoService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        expedienteService = mock(ExpedienteService.class);
        documentoService = mock(DocumentoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ExpedienteController(expedienteService, documentoService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ExpedienteResponseDTO sampleExpediente(Long id) {
        return new ExpedienteResponseDTO(id, 1L, "Demanda laboral", "Descripcion",
                EstadoExpediente.OPEN, LocalDate.now(), null, LocalDateTime.now(), LocalDateTime.now());
    }

    private DocumentoResponseDTO sampleDocumento(Long id, Long caseId) {
        return new DocumentoResponseDTO(id, caseId, "contrato.pdf", "application/pdf", "/uploads/contrato.pdf",
                "CONTRACT", 1024L, EstadoProcesamiento.UPLOADED, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void registrarCreaElExpedienteYDevuelve201() throws Exception {
        when(expedienteService.registrar(any())).thenReturn(sampleExpediente(1L));

        mvc.perform(post("/api/cases").contentType("application/json").content("""
                        {"clientId":1,"title":"Demanda laboral"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(1))
                .andExpect(jsonPath("$.title").value("Demanda laboral"));
    }

    @Test
    void registrarSinTituloDevuelve400() throws Exception {
        mvc.perform(post("/api/cases").contentType("application/json").content("""
                        {"clientId":1}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(expedienteService);
    }

    @Test
    void listarDevuelveLosExpedientesDelUsuarioAutenticado() throws Exception {
        when(expedienteService.listarPorUsuarioAutenticado()).thenReturn(List.of(sampleExpediente(1L), sampleExpediente(2L)));

        mvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorIdInexistenteDevuelve404() throws Exception {
        when(expedienteService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Expediente no encontrado"));

        mvc.perform(get("/api/cases/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarDevuelveElExpedienteActualizado() throws Exception {
        when(expedienteService.actualizar(eq(1L), any())).thenReturn(sampleExpediente(1L));

        mvc.perform(put("/api/cases/1").contentType("application/json").content("""
                        {"clientId":1,"title":"Demanda laboral actualizada"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(1));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete("/api/cases/1"))
                .andExpect(status().isNoContent());

        verify(expedienteService).eliminar(1L);
    }

    @Test
    void registrarDocumentoDevuelve201() throws Exception {
        when(documentoService.registrar(eq(1L), any())).thenReturn(sampleDocumento(10L, 1L));

        mvc.perform(post("/api/cases/1/documents").contentType("application/json").content("""
                        {"fileName":"contrato.pdf","fileType":"application/pdf"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(10));
    }

    @Test
    void listarDocumentosDevuelveElListado() throws Exception {
        when(documentoService.listarPorExpediente(1L)).thenReturn(List.of(sampleDocumento(10L, 1L)));

        mvc.perform(get("/api/cases/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void subirArchivoDevuelve201() throws Exception {
        when(documentoService.subirArchivo(eq(1L), any(), eq("CONTRACT"))).thenReturn(sampleDocumento(11L, 1L));
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "contenido".getBytes());
        MockMultipartFile category = new MockMultipartFile("category", "", "text/plain", "CONTRACT".getBytes());

        mvc.perform(multipart("/api/cases/1/documents/upload").file(file).file(category))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(11));
    }
}
