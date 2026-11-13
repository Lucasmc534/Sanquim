package br.com.sanquim.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
/** Recebe e valida as credenciais de login antes de delegar à camada de autenticação. */
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthService.LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(new AuthService.LoginRequest(request.email(), request.password()));
    }

    /** Credenciais submetidas ao endpoint de autenticação. */
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }
}
