package pe.edu.upc.legalai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI legalAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LegalAI API")
                        .description("API REST para la gestión de roles, usuarios, clientes y documentos de LegalAI")
                        .version("v1")
                        .contact(new Contact()
                                .name("LegalAI Team")
                                .email("contacto@legalai.com"))
                        .license(new License()
                                .name("Uso académico")));
    }
}
