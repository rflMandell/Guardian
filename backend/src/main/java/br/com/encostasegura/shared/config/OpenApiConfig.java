package br.com.encostasegura.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI encostaSeguraOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Encosta Segura API")
                        .description("Sistema de Alertas de Deslizamento e Alagamento com IoT e Inteligência Artificial.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Equipe Encosta Segura")
                                .email("contato@encostasegura.com.br"))
                        .license(new License()
                                .name("Proprietário / Acadêmico")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .schemaRequirement(BEARER_AUTH, new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Insira o token JWT no formato: Bearer <token>"));
    }
}
