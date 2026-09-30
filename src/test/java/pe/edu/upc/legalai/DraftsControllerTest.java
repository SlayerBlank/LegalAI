package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upc.legalai.controllers.DraftsController;
import pe.edu.upc.legalai.entities.Drafts;
import pe.edu.upc.legalai.servicesinterfaces.DraftsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DraftsControllerTest {

    private DraftsService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(DraftsService.class);
        DraftsController controller = new DraftsController();
        ReflectionTestUtils.setField(controller, "dS", service);
        ReflectionTestUtils.setField(controller, "modelMapper", new ModelMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void insertMapeaElRequestYLlamaAlServicio() throws Exception {
        mvc.perform(post("/drafts").contentType("application/json").content("""
                        {"expediente_id":1,"usuario_id":2,"title":"Contrato inicial",
                         "prompt":"Genera un contrato","content":"Contenido generado","status":"DRAFT"}
                        """))
                .andExpect(status().isOk());

        ArgumentCaptor<Drafts> captor = ArgumentCaptor.forClass(Drafts.class);
        verify(service).insert(captor.capture());
        Drafts saved = captor.getValue();
        assertEquals("Contrato inicial", saved.getTitle());
        assertEquals("Contenido generado", saved.getContent());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(1L, saved.getExpediente().getCaseId());
        assertEquals(2L, saved.getCreatedBy().getUserId());
    }

    @Test
    void listDevuelveLosDraftsMapeados() throws Exception {
        Drafts draft = new Drafts();
        draft.setDraftId(5L);
        draft.setTitle("Borrador");
        draft.setContent("Texto");
        draft.setStatus("DRAFT");
        draft.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(service.list()).thenReturn(List.of(draft));

        mvc.perform(get("/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].draftId").value(5))
                .andExpect(jsonPath("$[0].title").value("Borrador"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void listSinDraftsDevuelveListaVacia() throws Exception {
        when(service.list()).thenReturn(List.of());

        mvc.perform(get("/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
