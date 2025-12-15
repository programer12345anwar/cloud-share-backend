package com.anwar.cloudshareapi.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudShareApiDocumentation() {

        return new OpenAPI()
                .info(new Info()
                        .title("📦 CloudShare API")
                        .description(
                                "CloudShare is a **file-sharing and credits-based storage platform**. " +
                                        "This API manages **file uploads, transactions, payments, user credits, and webhook events**.\n\n" +
                                        "Controllers included:\n" +
                                        "• ClerkWebhookController\n" +
                                        "• FileController\n" +
                                        "• PaymentController\n" +
                                        "• ProfileController\n" +
                                        "• TransactionController\n" +
                                        "• UserCreditsController\n"
                        )
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Md Anwar Alam")
                                .email("mdanwar40212@gmail.com")
                                .url("https://github.com/programer12345anwar"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                )
                .externalDocs(new ExternalDocumentation()
                        .description("📘 CloudShare Full Documentation")
                        .url("http://cloudshare-docs.example.com"));
    }
}

