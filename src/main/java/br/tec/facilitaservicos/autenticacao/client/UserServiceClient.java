package br.tec.facilitaservicos.autenticacao.client;

import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import reactor.core.publisher.Mono;

public interface UserServiceClient {

    Mono<UsuarioDTO> findByEmailOrNomeUsuario(String emailOrNomeUsuario);

    Mono<UsuarioDTO> findById(Long id);

    Mono<Integer> updateTentativasLoginFalidas(Long id, int tentativas);

    Mono<Long> countUsuariosAtivos();
}