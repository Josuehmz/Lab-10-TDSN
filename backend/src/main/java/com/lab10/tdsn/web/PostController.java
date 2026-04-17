package com.lab10.tdsn.web;

import com.lab10.tdsn.dto.CreatePostRequest;
import com.lab10.tdsn.dto.PostResponse;
import com.lab10.tdsn.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Posts y stream")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/stream")
    @Operation(summary = "Stream global (público)", description = "Devuelve todos los posts ordenados del más reciente al más antiguo. No requiere autenticación.")
    public List<PostResponse> stream() {
        return postService.getPublicStream();
    }

    @GetMapping("/posts")
    @Operation(summary = "Listar posts (público)", description = "Alias del stream global. No requiere autenticación.")
    public List<PostResponse> posts() {
        return postService.getPublicStream();
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear post", description = "Requiere JWT con scope `write:posts` y audience de la API.")
    @SecurityRequirement(name = "bearer-jwt")
    public PostResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePostRequest body
    ) {
        return postService.createPost(jwt, body);
    }
}
