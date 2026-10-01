package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.RAGController;
import pe.edu.upc.legalai.servicesinterfaces.RAGService;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RAGControllerTest {
    final RAGService service = mock(RAGService.class);
    final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new RAGController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"question\":\" \"}", "{\"question\":null}", "{\"question\":\"q\",\"topK\":0}",
            "{\"question\":\"q\",\"topK\":11}", "{\"question\":\"q\",\"topK\":null}",
            "{\"question\":\"q\",\"userId\":9}", "{\"question\":\"q\",\"context\":\"fake\"}",
            "{\"question\":\"q\",\"chunkIds\":[9]}", "{\"question\":\"q\",\"ownerId\":9}"})
    void rejectsInvalidOrClientSuppliedContext(String body) throws Exception {
        for (String path : new String[]{"/api/documents/1/ask","/api/cases/1/ask"})
            mvc.perform(post(path).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void acceptsOptionalTopKAndRejectsLongQuestions() throws Exception {
        mvc.perform(post("/api/documents/1/ask").contentType("application/json").content("{\"question\":\"q\"}"))
                .andExpect(status().isOk());
        verify(service).preguntarDocumento(eq(1L),argThat(r -> r.getTopK()==null));
        mvc.perform(post("/api/cases/1/ask").contentType("application/json").content("{\"question\":\""+"x".repeat(2001)+"\"}"))
                .andExpect(status().isBadRequest());
        verify(service,never()).preguntarExpediente(any(),any());
    }
}
