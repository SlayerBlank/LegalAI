package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.edu.upc.legalai.controllers.ExpedienteController;
import pe.edu.upc.legalai.exceptions.*;
import pe.edu.upc.legalai.securities.*;
import pe.edu.upc.legalai.servicesinterfaces.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExpedienteController.class, properties = "app.jwt.secret=test-only-secret-for-upload-security-tests-2026")
@Import({WebSecurityConfig.class, JwtRequestFilter.class, JwtAuthenticationEntryPoint.class, JwtTokenUtil.class})
class DocumentoUploadWebTest {
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired org.springframework.security.web.FilterChainProxy security;
    @Autowired JwtTokenUtil tokens;
    @MockitoBean DocumentoService documents;
    @MockitoBean ExpedienteService cases;
    @MockitoBean UsuarioDetailsService users;
    private static final String URL = "/api/cases/12/documents/upload";

    private MockMvc mvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }
    private String bearer() {
        when(users.loadUserByUsername("upload@example.test"))
                .thenReturn(User.withUsername("upload@example.test").password("unused").roles("USER").build());
        return "Bearer " + tokens.generateToken("upload@example.test");
    }
    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "x.pdf", "application/pdf", "%PDF-1.7\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void requiresJwtAndAcceptsMultipartWithOptionalCategory() throws Exception {
        var mvc = mvc();
        mvc.perform(multipart(URL).file(pdf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart(URL).file(pdf()).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(documents);
        mvc.perform(multipart(URL).file(pdf())
                        .file(new MockMultipartFile("category", "", "text/plain", "Contrato".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .header("Authorization", bearer()))
                .andExpect(status().isCreated());
        verify(documents).subirArchivo(eq(12L), any(), eq("Contrato"));
        mvc.perform(multipart(URL).file(pdf()).header("Authorization", bearer())).andExpect(status().isCreated());
        verify(documents).subirArchivo(eq(12L), any(), isNull());
    }

    @Test
    void missingFileUsesErrorResponse() throws Exception {
        mvc().perform(multipart(URL).header("Authorization", bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(URL));
        verifyNoInteractions(documents);
    }

    @Test
    void uploadErrorsUseConsistentContractWithoutPrivateDetails() throws Exception {
        RuntimeException[] failures = {new BadRequestException("PDF invalido"), new ResourceNotFoundException("Expediente no encontrado"),
                new MaxUploadSizeExceededException(1024), new DocumentUploadException(new java.io.IOException("private-path"))};
        int[] statuses = {400, 404, 413, 500};
        for (int i = 0; i < failures.length; i++) {
            when(documents.subirArchivo(eq(12L), any(), isNull())).thenThrow(failures[i]);
            mvc().perform(multipart(URL).file(pdf()).header("Authorization", bearer()))
                    .andExpect(status().is(statuses[i])).andExpect(jsonPath("$.status").value(statuses[i]))
                    .andExpect(jsonPath("$.path").value(URL)).andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-path"))));
        }
    }
}
