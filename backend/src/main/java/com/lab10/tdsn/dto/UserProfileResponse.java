package com.lab10.tdsn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil del usuario autenticado")
public record UserProfileResponse(
        @Schema(description = "Subject (sub) del JWT")
        String sub,
        @Schema(example = "usuario@ejemplo.com")
        String email,
        @Schema(example = "Usuario Demo")
        String name,
        @Schema(description = "URL de avatar")
        String picture
) {
}
