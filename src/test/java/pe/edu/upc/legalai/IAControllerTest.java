package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.IAController;
import pe.edu.upc.legalai.dtos.response.IAResponseDTO;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.exceptions.IAServiceException;
import pe.edu.upc.legalai.servicesinterfaces.IAService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IAControllerTest {
    private IAService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(IAService.class);
        mvc = MockMvcBuilders.standaloneSetup(new IAController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsLegalAiContract() throws Exception {
        IAResponseDTO response = new IAResponseDTO();
        response.setAnswer("Respuesta");
        response.setProvider("alternative-provider");
        response.setModel("alternative-model");
        when(service.generarRespuesta(any())).thenReturn(response);
        mvc.perform(post("/api/ai/test").contentType("application/json").content("{\"prompt\":\"Pregunta\"}"))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"answer":"Respuesta","provider":"alternative-provider","model":"alternative-model"}
                        """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"prompt\":null}", "{\"prompt\":\"\"}", "{\"prompt\":\"   \"}", "not-json"})
    void rejectsInvalidPrompt(String body) throws Exception {
        mvc.perform(post("/api/ai/test").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsOversizedPrompt() throws Exception {
        mvc.perform(post("/api/ai/test").contentType("application/json")
                        .content("{\"prompt\":\"" + "x".repeat(10001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void serviceUnavailableUsesExistingErrorContract() throws Exception {
        when(service.generarRespuesta(any())).thenThrow(new IAServiceException());
        mvc.perform(post("/api/ai/test").contentType("application/json").content("{\"prompt\":\"Pregunta\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("status").value(503))
                .andExpect(jsonPath("error").value("Service Unavailable"))
                .andExpect(jsonPath("message").value("El servicio de inteligencia artificial no está disponible."))
                .andExpect(jsonPath("timestamp").exists())
                .andExpect(jsonPath("path").value("/api/ai/test"));
    }
}
