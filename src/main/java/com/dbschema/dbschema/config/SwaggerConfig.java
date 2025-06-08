package com.dbschema.dbschema.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Schema Management POC API")
                        .description("Dynamic Database Schema Generation and Management System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("POC Team")
                                .email("contact@example.com")
                                .url("www.example.com")));
    }
}