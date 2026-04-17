package com.lab10.tdsn.service;

import com.lab10.tdsn.dto.CreatePostRequest;
import com.lab10.tdsn.dto.PostResponse;
import com.lab10.tdsn.entity.AppUser;
import com.lab10.tdsn.entity.Post;
import com.lab10.tdsn.repo.AppUserRepository;
import com.lab10.tdsn.repo.PostRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final AppUserRepository appUserRepository;
    private final UserSyncService userSyncService;

    public PostService(
            PostRepository postRepository,
            AppUserRepository appUserRepository,
            UserSyncService userSyncService
    ) {
        this.postRepository = postRepository;
        this.appUserRepository = appUserRepository;
        this.userSyncService = userSyncService;
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPublicStream() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        Set<String> authorIds = posts.stream().map(Post::getAuthorId).collect(Collectors.toSet());
        Map<String, String> names = appUserRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(AppUser::getId, u -> u.getName() != null ? u.getName() : u.getId()));
        return posts.stream()
                .map(p -> new PostResponse(
                        p.getId(),
                        p.getContent(),
                        p.getAuthorId(),
                        p.getCreatedAt(),
                        names.getOrDefault(p.getAuthorId(), null)
                ))
                .toList();
    }

    @Transactional
    public PostResponse createPost(Jwt jwt, CreatePostRequest request) {
        AppUser user = userSyncService.upsertFromJwt(jwt);
        Post saved = postRepository.save(new Post(request.content().trim(), user.getId()));
        return new PostResponse(
                saved.getId(),
                saved.getContent(),
                saved.getAuthorId(),
                saved.getCreatedAt(),
                user.getName()
        );
    }
}
