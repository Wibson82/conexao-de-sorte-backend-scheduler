package br.tec.facilitaservicos.autenticacao.service;

import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import br.tec.facilitaservicos.autenticacao.exception.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Serviço para validação de usuários durante o processo de autenticação.
 */
@Service
public class UserValidationService {

    private final PasswordEncoder passwordEncoder;

    public UserValidationService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Valida a senha e o estado do usuário.
     *
     * @param usuario O DTO do usuário a ser validado.
     * @param senha A senha fornecida pelo usuário.
     * @return Um Mono contendo o UsuarioDTO se a validação for bem-sucedida, ou um Mono.error com AuthenticationException caso contrário.
     */
    public Mono<UsuarioDTO> validateUser(UsuarioDTO usuario, String senha) {
        return Mono.fromCallable(() -> {
            // Validar senha
            if (!passwordEncoder.matches(senha, usuario.getPassword())) {
                throw new AuthenticationException("Senha inválida");
            }

            // Validar estado do usuário
            if (!usuario.isAtivo() || usuario.isContaBloqueada() || !usuario.isEmailVerificado()) {
                if (usuario.isContaBloqueada()) {
                    throw new AuthenticationException("Conta bloqueada");
                }
                if (!usuario.isAtivo()) {
                    throw new AuthenticationException("Conta inativa");
                }
                if (!usuario.isEmailVerificado()) {
                    throw new AuthenticationException("Email não verificado");
                }
            }
            return usuario;
        });
    }
}
