package com.lab10.tdsn.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Combina scopes estándar ({@code scope}) con permisos RBAC de Auth0 ({@code permissions})
 * para que coincidan con {@code hasAuthority("SCOPE_…")} en SecurityConfig.
 */
public final class JwtAuth0AuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> out = new HashSet<>(scopes.convert(jwt));
        Object raw = jwt.getClaim("permissions");
        if (raw instanceof Collection<?> coll) {
            for (Object p : coll) {
                if (p == null) {
                    continue;
                }
                String name = p.toString().trim();
                if (!name.isEmpty()) {
                    out.add(new SimpleGrantedAuthority("SCOPE_" + name));
                }
            }
        } else if (raw instanceof String s && !s.isBlank()) {
            for (String part : s.split(",")) {
                String name = part.trim();
                if (!name.isEmpty()) {
                    out.add(new SimpleGrantedAuthority("SCOPE_" + name));
                }
            }
        }
        return new ArrayList<>(out);
    }

    public static List<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    }
}
