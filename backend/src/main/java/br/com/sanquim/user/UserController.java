package br.com.sanquim.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
/** Administra contas e fornece opções de alunos às funções escolares autorizadas. */
public class UserController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserController(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('SECRETARIA')")
    public List<UserResponse> list() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    public List<StudentOption> listStudents() {
        return users.findAllByRoleOrderByNameAsc(UserRole.ALUNO).stream()
            .map(user -> new StudentOption(user.getId(), user.getName()))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('SECRETARIA')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        // BCrypt considera no máximo 72 bytes; validar bytes evita truncamento silencioso.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A senha ultrapassa o limite de bytes suportado pelo BCrypt.");
        }
        String email = request.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail.");
        }
        AppUser user = users.save(new AppUser(request.name(), email,
            passwordEncoder.encode(request.password()), request.role()));
        return UserResponse.from(user);
    }

    /** Dados de criação; a senha é validada e transformada em hash antes de persistir. */
    public record CreateUserRequest(@NotBlank @Size(max = 120) String name,
                                    @NotBlank @Email @Size(max = 190) String email,
                                    @NotBlank @Size(min = 12, max = 72) String password,
                                    @NotNull UserRole role) {
    }

    /** Dados de conta retornados pela API, sem incluir material de autenticação. */
    public record UserResponse(UUID id, String name, String email, UserRole role) {
        static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        }
    }

    /** Opção mínima usada em formulários escolares autorizados. */
    public record StudentOption(UUID id, String name) {
    }
}
