package com.lab10.tdsn.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Lab 10 — API tipo Twitter",
                version = "1.0",
                description = "Stream público de posts. Autenticación con JWT Bearer (Auth0). " +
                        "Scopes recomendados: read:posts, write:posts, read:profile."
        )
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Token de acceso de Auth0 (audience = API identifier)"
)
public class OpenApiConfig {
}
