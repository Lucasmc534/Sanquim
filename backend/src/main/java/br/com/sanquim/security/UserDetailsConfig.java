package br.com.sanquim.security;

import br.com.sanquim.user.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
/** Carrega contas do banco para a autenticação de e-mail e senha. */
public class UserDetailsConfig {
    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos."));
    }
}
