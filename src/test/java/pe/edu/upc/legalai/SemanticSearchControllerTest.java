package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.SemanticSearchController;
import pe.edu.upc.legalai.dtos.request.SemanticSearchRequestDTO;
import pe.edu.upc.legalai.dtos.response.SemanticSearchResultDTO;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SemanticSearchControllerTest {

    private SemanticSearchService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(SemanticSearchService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SemanticSearchController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void buscarEnDocumentoDevuelveLosFragmentosHU101() throws Exception {
        when(service.searchDocument(eq(7L), any(SemanticSearchRequestDTO.class))).thenReturn(List.of(
                new SemanticSearchResultDTO(11L, 7L, 0, "Clausula de penalidad", 0.12, "contrato.pdf", 0, 21)));

        mvc.perform(post("/api/documents/7/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"penalidad por incumplimiento\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chunkId").value(11))
                .andExpect(jsonPath("$[0].documentName").value("contrato.pdf"));
    }

    @Test
    void buscarEnExpedienteDelegaEnElServicioHU101() throws Exception {
        when(service.searchCase(eq(3L), any(SemanticSearchRequestDTO.class))).thenReturn(List.of());

        mvc.perform(post("/api/cases/3/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"plazo de prescripcion\",\"topK\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void consultaVaciaSeRechazaSinLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/documents/7/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\",\"topK\":5}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).searchDocument(any(), any());
    }

    @Test
    void topKFueraDeRangoSeRechaza() throws Exception {
        mvc.perform(post("/api/documents/7/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"plazo\",\"topK\":50}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).searchDocument(any(), any());
    }

    @Test
    void documentoAjenoDevuelve404() throws Exception {
        when(service.searchDocument(eq(99L), any(SemanticSearchRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Documento no encontrado"));

        mvc.perform(post("/api/documents/99/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"plazo\",\"topK\":5}"))
                .andExpect(status().isNotFound());
    }
}
