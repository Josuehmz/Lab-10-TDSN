package com.lab10.tdsn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Post en el stream público")
public record PostResponse(
        @Schema(example = "1")
        Long id,
        @Schema(example = "Hola mundo")
        String content,
        @Schema(description = "Identificador del autor (sub de Auth0)")
        String authorId,
        @Schema(example = "2026-04-16T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Nombre para mostrar si el usuario existe en BD")
        String authorName
) {
}
