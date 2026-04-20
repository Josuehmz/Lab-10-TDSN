package com.lab10.ms.auth;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.RSAKeyProvider;

import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Valida JWT de Auth0 (issuer, audience, firma JWKS) sin Spring.
 */
public final class Auth0JwtVerifier {

    private final JWTVerifier verifier;
    private final JwkProvider jwkProvider;

    public Auth0JwtVerifier(String auth0Domain, String audience) {
        String domain = auth0Domain == null ? "" : auth0Domain.trim();
        if (domain.startsWith("https://")) {
            domain = URI.create(domain).getHost();
        }
        if (domain.isEmpty()) {
            throw new IllegalArgumentException("AUTH0_DOMAIN requerido");
        }
        String issuer = "https://" + domain + "/";
        try {
            URI jwksUri = URI.create("https://" + domain + "/.well-known/jwks.json");
            this.jwkProvider = new JwkProviderBuilder(jwksUri.toURL()).build();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar JWKS", e);
        }

        RSAKeyProvider keyProvider = new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String kid) {
                try {
                    return (RSAPublicKey) jwkProvider.get(kid).getPublicKey();
                } catch (JwkException e) {
                    throw new IllegalArgumentException("JWK inválido", e);
                }
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                return null;
            }

            @Override
            public String getPrivateKeyId() {
                return null;
            }
        };

        Algorithm algorithm = Algorithm.RSA256(keyProvider);
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
    }

    public DecodedJWT verifyBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization Bearer requerido");
        }
        String token = authorizationHeader.substring(7).trim();
        return verifier.verify(token);
    }

    public static String claimString(DecodedJWT jwt, String name) {
        Claim c = jwt.getClaim(name);
        if (c == null || c.isNull()) {
            return null;
        }
        return c.asString();
    }

    public static boolean hasScope(DecodedJWT jwt, String scope) {
        if (scope == null || scope.isBlank()) {
            return false;
        }
        String claim = jwt.getClaim("scope").asString();
        if (claim == null || claim.isBlank()) {
            return false;
        }
        Set<String> scopes = new HashSet<>(Arrays.asList(claim.split(" ")));
        return scopes.contains(scope);
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
