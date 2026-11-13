package br.com.sanquim.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garante que a configuração JWT rejeita segredos ausentes ou fracos. */
class SecurityConfigTest {
    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void acceptsRandomSecretOfAtLeast32Bytes() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        assertNotNull(securityConfig.jwtSecretKey(secret));
    }

    @Test
    void rejectsMissingOrShortSecret() {
        assertThrows(IllegalStateException.class, () -> securityConfig.jwtSecretKey(""));
        assertThrows(IllegalStateException.class,
            () -> securityConfig.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[31])));
    }

    @Test
    void configuresJwtDecoderWithTheSharedSecret() {
        var key = securityConfig.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[32]));
        JwtDecoder decoder = securityConfig.jwtDecoder(key);
        assertNotNull(decoder);
    }

    @Test
    void mapsJwtRolesToSpringAuthorities() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "HS256")
            .subject("teacher@example.com")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .claim("roles", List.of("PROFESSOR"))
            .build();

        var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
    }
}
