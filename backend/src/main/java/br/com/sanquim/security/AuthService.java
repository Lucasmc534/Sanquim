package br.com.sanquim.security;

import br.com.sanquim.user.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
/** Valida credenciais e emite tokens Bearer com identidade e papel da conta. */
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder,
                       @Value("${sanquim.security.jwt-expiration-minutes}") long expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        if (expirationMinutes < 1) {
            throw new IllegalStateException("JWT_EXPIRATION_MINUTES must be at least 1.");
        }
        this.expirationMinutes = expirationMinutes;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
        AppUser user = (AppUser) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        // A autorização posterior usa estes claims assinados, nunca um papel enviado pelo cliente.
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sanquim-api")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getEmail())
            .claim("roles", List.of(user.getRole().name()))
            .claim("userId", user.getId().toString())
            .claim("name", user.getName())
            .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresAt, user.getId(), user.getName(),
            user.getEmail(), user.getRole().name());
    }

    /** Entrada da autenticação após validação HTTP do controller. */
    public record LoginRequest(String email, String password) {
    }

    /** Resultado do login com token temporário e informações de perfil. */
    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                                java.util.UUID userId, String name, String email, String role) {
    }
}
