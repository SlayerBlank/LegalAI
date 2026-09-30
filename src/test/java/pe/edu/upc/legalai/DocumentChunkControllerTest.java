package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.DTOs.response.ChunkGenerationResponseDTO;
import pe.edu.upc.legalai.DTOs.response.DocumentChunkResponseDTO;
import pe.edu.upc.legalai.controllers.DocumentChunkController;
import pe.edu.upc.legalai.servicesinterfaces.DocumentChunkService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentChunkControllerTest {

    private DocumentChunkService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(DocumentChunkService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DocumentChunkController(service)).build();
    }

    @Test
    void generarDevuelveElResumenDeLaGeneracion() throws Exception {
        when(service.generar(1L)).thenReturn(new ChunkGenerationResponseDTO(1L, 4, 2500, 300));

        mvc.perform(post("/api/documents/1/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.chunksCreated").value(4))
                .andExpect(jsonPath("$.chunkSize").value(2500));
    }

    @Test
    void listarDevuelveLosChunksOrdenados() throws Exception {
        when(service.listar(1L)).thenReturn(List.of(
                new DocumentChunkResponseDTO(1L, 1L, 0, "Primer fragmento", 0, 17),
                new DocumentChunkResponseDTO(2L, 1L, 1, "Segundo fragmento", 17, 35)
        ));

        mvc.perform(get("/api/documents/1/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chunkIndex").value(0))
                .andExpect(jsonPath("$[1].chunkIndex").value(1));
    }

    @Test
    void listarSinChunksDevuelveListaVacia() throws Exception {
        when(service.listar(2L)).thenReturn(List.of());

        mvc.perform(get("/api/documents/2/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
