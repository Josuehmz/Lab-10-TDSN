package com.lab10.tdsn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Cuerpo para crear un post (máx. 140 caracteres)")
public record CreatePostRequest(
        @NotBlank
        @Size(max = 140)
        @Schema(example = "Hola desde el Lab 10", maxLength = 140)
        String content
) {
}
