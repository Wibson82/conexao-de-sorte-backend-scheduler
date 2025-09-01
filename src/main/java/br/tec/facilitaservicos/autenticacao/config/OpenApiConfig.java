
package br.tec.facilitaservicos.autenticacao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("API de Microsserviços")
                        .description("Documentação da API para os Microsserviços do Conexão de Sorte")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addTagsItem(new Tag().name("Health Check").description("Endpoints for health checking"))
                .path("/health", new PathItem()
                        .get(new Operation()
                                .addTagsItem("Health Check")
                                .summary("Health Check Endpoint")
                                .description("Returns the health status of the service.")
                                .operationId("healthCheck")
                                .responses(new ApiResponses()
                                        .addApiResponse("200", new ApiResponse()
                                                .description("Service is healthy")
                                                .content(new Content()
                                                        .addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                                                                .schema(new Schema<>()
                                                                        .type("object")
                                                                        .addProperty("status", new Schema<>().type("string").example("UP"))))))
                                        .addApiResponse("503", new ApiResponse()
                                                .description("Service is unhealthy")
                                                .content(new Content()
                                                        .addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                                                                .schema(new Schema<>()
                                                                        .type("object")
                                                                        .addProperty("status", new Schema<>().type("string").example("DOWN")))))))));
    }
}
