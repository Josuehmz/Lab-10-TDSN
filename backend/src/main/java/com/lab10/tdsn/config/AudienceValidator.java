package com.lab10.tdsn.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> requiredAudiences;

    public AudienceValidator(Set<String> requiredAudiences) {
        this.requiredAudiences = requiredAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (requiredAudiences == null || requiredAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }

        for (String aud : token.getAudience()) {
            if (requiredAudiences.contains(aud)) {
                return OAuth2TokenValidatorResult.success();
            }
        }

        OAuth2Error err = new OAuth2Error(
                "invalid_token",
                "The required audience is missing",
                null
        );
        return OAuth2TokenValidatorResult.failure(err);
    }
}
