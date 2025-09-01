package br.tec.facilitaservicos.autenticacao.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import reactor.core.publisher.Mono;

@Component
public class UserServiceClientImpl implements UserServiceClient {

    private final WebClient webClient;

    public UserServiceClientImpl(@Value("${user-service.url}") String userServiceUrl, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(userServiceUrl).build();
    }

    @Override
    public Mono<UsuarioDTO> findByEmailOrNomeUsuario(String emailOrNomeUsuario) {
        return webClient.get()
                .uri("/rest/v1/users/search?emailOrUsername={emailOrUsername}", emailOrNomeUsuario)
                .retrieve()
                .bodyToMono(UsuarioDTO.class);
    }

    @Override
    public Mono<UsuarioDTO> findById(Long id) {
        return webClient.get()
                .uri("/rest/v1/users/{id}", id)
                .retrieve()
                .bodyToMono(UsuarioDTO.class);
    }

    @Override
    public Mono<Integer> updateTentativasLoginFalidas(Long id, int tentativas) {
        return webClient.put()
                .uri("/rest/v1/users/{id}/login-attempts?attempts={attempts}", id, tentativas)
                .retrieve()
                .bodyToMono(Integer.class);
    }

    @Override
    public Mono<Long> countUsuariosAtivos() {
        return webClient.get()
                .uri("/rest/v1/users/active/count")
                .retrieve()
                .bodyToMono(Long.class);
    }
}