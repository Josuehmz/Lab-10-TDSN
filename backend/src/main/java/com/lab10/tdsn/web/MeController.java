package com.lab10.tdsn.web;

import com.lab10.tdsn.dto.UserProfileResponse;
import com.lab10.tdsn.entity.AppUser;
import com.lab10.tdsn.service.UserSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Perfil")
public class MeController {

    private final UserSyncService userSyncService;

    public MeController(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario actual", description = "Requiere JWT con scope `read:profile`.")
    @SecurityRequirement(name = "bearer-jwt")
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = userSyncService.upsertFromJwt(jwt);
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl()
        );
    }
}
