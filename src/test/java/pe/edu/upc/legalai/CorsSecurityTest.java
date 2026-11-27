package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.Filter;
import pe.edu.upc.legalai.securities.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CorsSecurityTest {
    private static final String RENDER = "https://legalai-frontend-bnd5.onrender.com";

    @Configuration
    @EnableWebMvc
    @Import({CorsConfig.class, WebSecurityConfig.class, JwtRequestFilter.class, Probe.class})
    static class Config {
        @Bean JwtTokenUtil tokens() { return mock(JwtTokenUtil.class); }
        @Bean UsuarioDetailsService users() { return mock(UsuarioDetailsService.class); }
        @Bean JwtAuthenticationEntryPoint entryPoint() { return new JwtAuthenticationEntryPoint(new tools.jackson.databind.ObjectMapper()); }
    }

    // Isolate the real security chain from persistence and external services.
    @RestController
    static class Probe {
        @RequestMapping("/**")
        String ok() { return "ok"; }
    }

    private AnnotationConfigWebApplicationContext context(boolean configured) {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        if (configured) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "CORS_ALLOWED_ORIGINS=" + RENDER + ",http://localhost:3000,http://localhost:5173,http://localhost:4200");
        }
        context.register(Config.class);
        context.refresh();
        return context;
    }

    private MockMvc mvc(AnnotationConfigWebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @Test
    void preflightAllowsConfiguredOriginsAndAllEndpointMethodsWithoutJwt() throws Exception {
        try (var context = context(true)) {
            var mvc = mvc(context);
            String[][] endpoints = {{"POST", "/api/auth/login"}, {"POST", "/api/auth/register"},
                    {"GET", "/api/users/me"}, {"POST", "/api/cases/1/documents/upload"},
                    {"GET", "/api/documents/1/download"}, {"PATCH", "/api/chat/sessions/1"},
                    {"PUT", "/api/clients/1"}, {"DELETE", "/api/documents/1"}, {"OPTIONS", "/api/documents"}};
            for (String origin : new String[]{RENDER, "http://localhost:3000", "http://localhost:5173", "http://localhost:4200"}) {
                for (String[] endpoint : endpoints) {
                    mvc.perform(options(endpoint[1]).header("Origin", origin)
                                    .header("Access-Control-Request-Method", endpoint[0])
                                    .header("Access-Control-Request-Headers", "authorization,content-type"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("Access-Control-Allow-Origin", origin))
                            .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type"));
                }
            }
            verifyNoInteractions(context.getBean(JwtTokenUtil.class));
        }
    }

    @Test
    void rejectsUnlistedOrigin() throws Exception {
        try (var context = context(true)) {
            mvc(context).perform(options("/api/auth/login").header("Origin", "https://untrusted.example")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void localDefaultsWorkWithoutEnvironmentVariable() throws Exception {
        try (var context = context(false)) {
            for (String origin : new String[]{"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"}) {
                mvc(context).perform(options("/api/auth/login").header("Origin", origin)
                                .header("Access-Control-Request-Method", "POST"))
                        .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origin));
            }
        }
    }

    @Test
    void publicAuthAndAuthenticatedRequestsRetainCorsHeaders() throws Exception {
        try (var context = context(true)) {
            var mvc = mvc(context);
            for (String path : new String[]{"/api/auth/login", "/api/auth/register"}) {
                mvc.perform(post(path).header("Origin", RENDER)).andExpect(status().isOk());
            }
            mvc.perform(get("/api/users/me").header("Origin", RENDER))
                    .andExpect(status().isUnauthorized()).andExpect(header().string("Access-Control-Allow-Origin", RENDER));
            when(context.getBean(JwtTokenUtil.class).getEmailFromToken("test-token")).thenReturn("test@example.com");
            when(context.getBean(UsuarioDetailsService.class).loadUserByUsername("test@example.com"))
                    .thenReturn(org.springframework.security.core.userdetails.User.withUsername("test@example.com")
                            .password("unused").roles("USER").build());
            mvc.perform(get("/api/documents/1/download").header("Origin", RENDER).header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition, X-Request-Id"));
            mvc.perform(multipart("/api/cases/1/documents/upload").file("file", new byte[]{1, 2})
                            .header("Origin", RENDER).header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", RENDER));
        }
    }

    @Test
    void jwtFilterSkipsOptionsEvenWithInvalidBearerHeader() throws Exception {
        var tokens = mock(JwtTokenUtil.class);
        var users = mock(UsuarioDetailsService.class);
        var entryPoint = mock(JwtAuthenticationEntryPoint.class);
        var filter = new JwtRequestFilter(tokens, users, entryPoint);
        var request = new org.springframework.mock.web.MockHttpServletRequest("OPTIONS", "/api/users/me");
        request.addHeader("Authorization", "Bearer invalid");
        var chain = mock(jakarta.servlet.FilterChain.class);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(tokens, users, entryPoint);
    }
}
