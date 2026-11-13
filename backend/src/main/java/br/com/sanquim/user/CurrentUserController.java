package br.com.sanquim.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
/** Devolve o perfil derivado dos claims do JWT autenticado. */
public class CurrentUserController {
    @GetMapping
    public CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUserResponse(UUID.fromString(jwt.getClaimAsString("userId")),
            jwt.getClaimAsString("name"), jwt.getSubject(),
            UserRole.valueOf(jwt.getClaimAsStringList("roles").getFirst()));
    }

    /** Perfil associado ao JWT atual, sem aceitar identidade informada pelo cliente. */
    public record CurrentUserResponse(UUID id, String name, String email, UserRole role) {
    }
}
