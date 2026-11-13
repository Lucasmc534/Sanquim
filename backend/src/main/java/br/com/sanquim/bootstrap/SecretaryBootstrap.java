package br.com.sanquim.bootstrap;

import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.AppUserRepository;
import br.com.sanquim.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
/** Cria a primeira conta de secretaria somente quando o bootstrap foi configurado. */
public class SecretaryBootstrap implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String name;
    private final String password;
    private final boolean demoAccountsEnabled;

    public SecretaryBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder,
        @Value("${sanquim.bootstrap.secretaria-email}") String email,
        @Value("${sanquim.bootstrap.secretaria-name}") String name,
        @Value("${sanquim.bootstrap.secretaria-password}") String password,
        @Value("${sanquim.demo.accounts-enabled:false}") boolean demoAccountsEnabled) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.name = name;
        this.password = password;
        this.demoAccountsEnabled = demoAccountsEnabled;
    }

    @Override
    public void run(String... args) {
        if (demoAccountsEnabled) {
            return;
        }
        boolean configured = !email.isBlank() || !name.isBlank() || !password.isBlank();
        if (!configured) {
            return;
        }
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Configure email, nome e senha juntos para criar a conta inicial.");
        }
        if (users.count() > 0) {
            throw new IllegalStateException(
                "A conta inicial só pode ser provisionada em um banco vazio; remova as variáveis de bootstrap.");
        }
        // O bootstrap inicial nunca altera uma conta ou senha já existente.
        users.save(new AppUser(name, email, passwordEncoder.encode(password), UserRole.SECRETARIA));
    }
}
