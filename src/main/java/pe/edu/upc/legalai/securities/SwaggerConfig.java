package pe.edu.upc.legalai.securities;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import pe.edu.upc.legalai.exceptions.ErrorResponse;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI legalAiOpenAPI() {
        String schemeName = "Bearer Token";
        return new OpenAPI()
                .info(new Info()
                        .title("LegalAI API")
                        .description("Backend tradicional para gestion legal: usuarios, clientes, expedientes y documentos")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer legalAiResponseDocumentation() {
        return openApi -> {
            ModelConverters.getInstance().read(ErrorResponse.class).forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                errorResponse(operation, "500", "Error interno del servidor");
                if (path.startsWith("/api/auth/")) {
                    operation.setSecurity(List.of());
                } else {
                    errorResponse(operation, "401", "JWT ausente, invalido o vencido; usuario no habilitado");
                    errorResponse(operation, "403", "Sin autorizacion para realizar esta operacion");
                }
                if (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT || path.contains("{")) {
                    errorResponse(operation, "400", "Datos, identificadores o fechas invalidos");
                }
                if (path.contains("{") || path.equals("/api/cases") && method == PathItem.HttpMethod.POST) {
                    errorResponse(operation, "404", "Recurso inexistente o perteneciente a otro usuario");
                }
                if (path.equals("/api/auth/register")) {
                    errorResponse(operation, "409", "Correo ya registrado");
                }
                if (path.equals("/api/auth/login")) {
                    errorResponse(operation, "401", "Credenciales invalidas o usuario no habilitado");
                }
                if (method == PathItem.HttpMethod.DELETE
                        && (path.startsWith("/api/clients/") || path.startsWith("/api/cases/"))) {
                    errorResponse(operation, "409", "El recurso tiene registros dependientes; eliminelos primero");
                }
            }));
        };
    }

    private void errorResponse(Operation operation, String code, String description) {
        operation.getResponses().addApiResponse(code, new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
    }
}
