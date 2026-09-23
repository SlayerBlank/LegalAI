package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import pe.edu.upc.legalai.exceptions.GlobalExceptionHandler;
import pe.edu.upc.legalai.securities.JwtAuthenticationEntryPoint;
import pe.edu.upc.legalai.securities.JwtTokenUtil;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorHandlerTest {

    @Test
    void internalErrorsDoNotExposeImplementationDetails() {
        var response = new GlobalExceptionHandler().handleGeneric(
                new IllegalStateException("private-database-information"), new MockHttpServletRequest("GET", "/api/clients"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Error interno del servidor");
        assertThat(response.getBody().getPath()).isEqualTo("/api/clients");
    }

    @Test
    void forbiddenResponsesUseSameErrorContractInMvcAndSecurity() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/roles");
        var exception = new AccessDeniedException("private-authority-detail");
        var mvc = new GlobalExceptionHandler().handleAccessDenied(exception, request);
        var response = new MockHttpServletResponse();
        var mapper = JsonMapper.builder().findAndAddModules().build();
        new JwtAuthenticationEntryPoint(mapper).handle(request, response, exception);
        var body = mapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.path("status").asInt()).isEqualTo(mvc.getStatusCode().value());
        assertThat(body.path("message").asText()).isEqualTo(mvc.getBody().getMessage());
        assertThat(body.path("path").asText()).isEqualTo("/api/roles");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    @Test
    void jwtRejectsWeakConfiguration() {
        var mapper = JsonMapper.builder().build();
        assertThatThrownBy(() -> new JwtTokenUtil("short", 3600000, mapper)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtTokenUtil("a".repeat(32), 0, mapper)).isInstanceOf(IllegalArgumentException.class);
    }
}
