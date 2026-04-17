package com.lab10.tdsn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab10.tdsn.dto.CreatePostRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getPosts_isPublic() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
    }

    @Test
    void getStream_isPublic() throws Exception {
        mockMvc.perform(get("/api/stream"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void postPosts_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("hola"))))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void postPosts_withWriteScope_isCreated() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("post de prueba")))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(j -> j.subject("auth0|test-user"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_write:posts"))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").value("post de prueba"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.authorId").value("auth0|test-user"));
    }

    @Test
    void me_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void me_withProfileScope_returnsOk() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(j -> j
                                        .subject("auth0|me-user")
                                        .claim("email", "me@test.com")
                                        .claim("name", "Usuario Me"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_read:profile"))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.sub").value("auth0|me-user"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("me@test.com"));
    }
}
