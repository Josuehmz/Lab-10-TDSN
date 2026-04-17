package com.lab10.tdsn.service;

import com.lab10.tdsn.entity.AppUser;
import com.lab10.tdsn.repo.AppUserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSyncService {

    private final AppUserRepository appUserRepository;

    public UserSyncService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser upsertFromJwt(Jwt jwt) {
        String id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("nickname"),
                email
        );
        String picture = jwt.getClaimAsString("picture");

        return appUserRepository.findById(id)
                .map(user -> {
                    user.updateProfile(email, name, picture);
                    return user;
                })
                .orElseGet(() -> appUserRepository.save(new AppUser(id, email, name, picture)));
    }

    private static String firstNonBlank(String... values) {
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
