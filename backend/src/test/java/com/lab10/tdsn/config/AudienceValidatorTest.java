package com.lab10.tdsn.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    @Test
    void validate_whenAudienceMatches_isSuccess() {
        AudienceValidator validator = new AudienceValidator(Set.of("https://lab10-tdsn-api"));
        Jwt jwt = Jwt.withTokenValue("t")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .subject("auth0|user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("aud", List.of("https://lab10-tdsn-api"))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void validate_whenAudienceMissing_isFailure() {
        AudienceValidator validator = new AudienceValidator(Set.of("https://lab10-tdsn-api"));
        Jwt jwt = Jwt.withTokenValue("t")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .subject("auth0|user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("aud", List.of("https://another-api"))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validate_whenNoRequiredAudiencesConfigured_isSuccess() {
        AudienceValidator validator = new AudienceValidator(Set.of());
        Jwt jwt = Jwt.withTokenValue("t")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .subject("auth0|user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("aud", List.of("anything"))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }
}
