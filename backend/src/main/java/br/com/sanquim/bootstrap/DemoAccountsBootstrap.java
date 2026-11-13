package br.com.sanquim.bootstrap;

import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.AppUserRepository;
import br.com.sanquim.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "sanquim.demo.accounts-enabled", havingValue = "true")
/** Provisiona contas @sanquim.test apenas quando o modo de demonstração foi habilitado. */
public class DemoAccountsBootstrap implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final List<DemoAccount> accounts;

    public DemoAccountsBootstrap(
        AppUserRepository users,
        PasswordEncoder passwordEncoder,
        @Value("${sanquim.demo.aluno-email}") String studentEmail,
        @Value("${sanquim.demo.aluno-name}") String studentName,
        @Value("${sanquim.demo.aluno-password}") String studentPassword,
        @Value("${sanquim.demo.professor-email}") String professorEmail,
        @Value("${sanquim.demo.professor-name}") String professorName,
        @Value("${sanquim.demo.professor-password}") String professorPassword,
        @Value("${sanquim.demo.secretaria-email}") String secretaryEmail,
        @Value("${sanquim.demo.secretaria-name}") String secretaryName,
        @Value("${sanquim.demo.secretaria-password}") String secretaryPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.accounts = List.of(
            new DemoAccount(studentEmail, studentName, studentPassword, UserRole.ALUNO),
            new DemoAccount(professorEmail, professorName, professorPassword, UserRole.PROFESSOR),
            new DemoAccount(secretaryEmail, secretaryName, secretaryPassword, UserRole.SECRETARIA));
    }

    @Override
    @Transactional
    public void run(String... args) {
        validateAccounts();
        // Reaplica as senhas configuradas apenas aos endereços reservados para demonstração.
        for (DemoAccount account : accounts) {
            users.findByEmailIgnoreCase(account.email())
                .ifPresentOrElse(
                    existing -> refreshExistingDemoAccount(existing, account),
                    () -> users.save(new AppUser(account.name(), account.email(),
                        passwordEncoder.encode(account.password()), account.role())));
        }
    }

    private void refreshExistingDemoAccount(AppUser existing, DemoAccount account) {
        if (existing.getRole() != account.role()) {
            throw new IllegalStateException("A conta de demonstração já existe com outro perfil.");
        }
        existing.changePasswordHash(passwordEncoder.encode(account.password()));
        users.save(existing);
    }

    private void validateAccounts() {
        Set<String> emails = new HashSet<>();
        for (DemoAccount account : accounts) {
            String normalizedEmail = account.email().trim().toLowerCase(Locale.ROOT);
            if (account.name().isBlank() || !normalizedEmail.endsWith("@sanquim.test")) {
                throw new IllegalStateException(
                    "As contas de demonstração precisam de nomes e e-mails @sanquim.test.");
            }
            if (!emails.add(normalizedEmail)) {
                throw new IllegalStateException("Os e-mails das contas de demonstração devem ser distintos.");
            }
            int passwordBytes = account.password().getBytes(StandardCharsets.UTF_8).length;
            if (account.password().length() < 12 || passwordBytes > 72) {
                throw new IllegalStateException(
                    "As senhas de demonstração devem ter pelo menos 12 caracteres e no máximo 72 bytes.");
            }
        }
    }

    private record DemoAccount(String email, String name, String password, UserRole role) {
    }
}
