package br.tec.facilitaservicos.autenticacao.configuracao;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class LocaleTimeConfig {

    @PostConstruct
    public void init() {
        Locale.setDefault(Locale.of("pt", "BR"));
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }
}

