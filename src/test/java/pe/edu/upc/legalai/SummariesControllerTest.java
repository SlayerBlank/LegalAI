package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.SummariesController;
import pe.edu.upc.legalai.entities.Summaries;
import pe.edu.upc.legalai.servicesinterfaces.ISummariesService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SummariesControllerTest {

    private ISummariesService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(ISummariesService.class);
        SummariesController controller = new SummariesController(service, new ModelMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void insertMapeaElRequestYLlamaAlServicio() throws Exception {
        mvc.perform(post("/summaries").contentType("application/json").content("""
                        {"documento_id":1,"expediente_id":2,"usuario_id":3,
                         "summaryType":"DOCUMENT","content":"Resumen generado"}
                        """))
                .andExpect(status().isOk());

        ArgumentCaptor<Summaries> captor = ArgumentCaptor.forClass(Summaries.class);
        verify(service).insert(captor.capture());
        Summaries saved = captor.getValue();
        assertEquals("Resumen generado", saved.getContent());
        assertEquals("DOCUMENT", saved.getSummaryType());
        assertEquals(1L, saved.getDocumento().getDocumentId());
        assertEquals(2L, saved.getExpediente().getCaseId());
        assertEquals(3L, saved.getGeneratedBy().getUserId());
    }

    @Test
    void listDevuelveLosResumenesMapeados() throws Exception {
        Summaries summary = new Summaries();
        summary.setSummaryId(7L);
        summary.setSummaryType("DOCUMENT");
        summary.setContent("Texto resumido");
        summary.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        when(service.list()).thenReturn(List.of(summary));

        mvc.perform(get("/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].summaryId").value(7))
                .andExpect(jsonPath("$[0].summaryType").value("DOCUMENT"))
                .andExpect(jsonPath("$[0].content").value("Texto resumido"));
    }

    @Test
    void listSinResumenesDevuelveListaVacia() throws Exception {
        when(service.list()).thenReturn(List.of());

        mvc.perform(get("/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
