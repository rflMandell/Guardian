package br.com.guardian.shared.config;

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
    public OpenAPI guardianOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Guardian API")
                        .description("Sistema Inteligente de Monitoramento de Riscos e Orientação para Evacuação Urbana.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Equipe Guardian")
                                .email("contato@guardian.com.br"))
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
