package br.com.sanquim.bootstrap;

import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.AppUserRepository;
import br.com.sanquim.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/** Verifica a criação de contas de demonstração e a validação do tamanho da senha. */
class DemoAccountsBootstrapTest {
    @Mock
    private AppUserRepository users;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void createsDemoAccountsWithEncodedPasswordsAndExpectedRoles() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DemoAccountsBootstrap bootstrap = bootstrap();

        bootstrap.run();

        ArgumentCaptor<AppUser> savedUsers = ArgumentCaptor.forClass(AppUser.class);
        verify(users, times(3)).save(savedUsers.capture());
        List<AppUser> accounts = savedUsers.getAllValues();
        assertEquals(List.of(UserRole.ALUNO, UserRole.PROFESSOR, UserRole.SECRETARIA),
            accounts.stream().map(AppUser::getRole).toList());
        assertTrue(passwordEncoder.matches("aluno12345678", accounts.get(0).getPassword()));
        assertTrue(passwordEncoder.matches("professor12345678", accounts.get(1).getPassword()));
        assertTrue(passwordEncoder.matches("secretaria12345678", accounts.get(2).getPassword()));
    }

    @Test
    void rejectsPasswordsBelowTheApplicationMinimumBeforeCreatingAccounts() {
        DemoAccountsBootstrap bootstrap = new DemoAccountsBootstrap(
            users, passwordEncoder,
            "aluno@sanquim.test", "Aluno Demonstração", "aluno123",
            "professor@sanquim.test", "Professor Demonstração", "professor12345678",
            "secretaria@sanquim.test", "Secretaria Demonstração", "secretaria12345678");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, bootstrap::run);
        verify(users, org.mockito.Mockito.never()).save(any(AppUser.class));
    }

    private DemoAccountsBootstrap bootstrap() {
        return new DemoAccountsBootstrap(
            users, passwordEncoder,
            "aluno@sanquim.test", "Aluno Demonstração", "aluno12345678",
            "professor@sanquim.test", "Professor Demonstração", "professor12345678",
            "secretaria@sanquim.test", "Secretaria Demonstração", "secretaria12345678");
    }
}
