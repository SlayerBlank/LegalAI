package pe.edu.upc.legalai;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.SemanticSearchController;
import pe.edu.upc.legalai.servicesimplements.SemanticSearchService;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SemanticSearchControllerTest {
    final SemanticSearchService service = mock(SemanticSearchService.class);
    final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new SemanticSearchController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"query\":\" \"}", "{\"query\":null}", "{\"query\":\"q\",\"topK\":0}",
            "{\"query\":\"q\",\"topK\":21}", "{\"query\":\"q\",\"topK\":null}"})
    void rejectsInvalidSearch(String body) throws Exception {
        for (String path : new String[]{"/api/documents/1/search", "/api/cases/1/search"})
            mvc.perform(post(path).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void defaultTopKAndForce() throws Exception {
        mvc.perform(post("/api/documents/1/search").contentType("application/json").content("{\"query\":\"q\"}"))
                .andExpect(status().isOk());
        verify(service).searchDocument(eq(1L), argThat(r -> r.getTopK() == 5));
        mvc.perform(post("/api/documents/1/embeddings")).andExpect(status().isOk());
        verify(service).generar(1L, false);
        mvc.perform(post("/api/documents/1/embeddings?force=true")).andExpect(status().isOk());
        verify(service).generar(1L, true);
    }
}
