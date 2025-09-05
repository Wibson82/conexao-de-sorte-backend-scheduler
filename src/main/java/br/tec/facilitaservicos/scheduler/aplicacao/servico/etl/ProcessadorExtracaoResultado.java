package br.tec.facilitaservicos.scheduler.aplicacao.servico.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processador de extração de resultados.
 * Versão reativa baseada no ProcessadorExtracaoResultado do backend original.
 */
@Service
public class ProcessadorExtracaoResultado {

    private static final Logger logger = LoggerFactory.getLogger(ProcessadorExtracaoResultado.class);
    
    // Padrões regex para extração de números
    private static final Pattern PATTERN_NUMEROS = Pattern.compile("\\b\\d{2}\\b");
    private static final Pattern PATTERN_CONCURSO = Pattern.compile("Concurso\\s+(\\d+)");
    private static final Pattern PATTERN_DATA = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})");

    /**
     * Processa o HTML extraído e retorna os dados estruturados.
     */
    public Mono<Map<String, Object>> processar(String modalidade, String html) {
        logger.debug("Processando HTML para modalidade: {}", modalidade);
        
        return Mono.fromCallable(() -> {
            Map<String, Object> resultado = new HashMap<>();
            
            try {
                // Extrair informações básicas
                resultado.put("modalidade", modalidade);
                resultado.put("dataExtracao", LocalDateTime.now());
                
                // Extrair número do concurso
                String numeroConcurso = extrairNumeroConcurso(html);
                if (numeroConcurso != null) {
                    resultado.put("concurso", numeroConcurso);
                }
                
                // Extrair data do sorteio
                LocalDate dataSorteio = extrairDataSorteio(html);
                if (dataSorteio != null) {
                    resultado.put("dataSorteio", dataSorteio);
                }
                
                // Extrair números sorteados
                var numerosSorteados = extrairNumerosSorteados(html, modalidade);
                if (!numerosSorteados.isEmpty()) {
                    resultado.put("numerosSorteados", numerosSorteados);
                }
                
                // Informações adicionais baseadas na modalidade
                processarModalidadeEspecifica(modalidade, html, resultado);
                
                logger.debug("Processamento concluído: modalidade={}, itens={}", modalidade, resultado.size());
                return resultado;
                
            } catch (Exception e) {
                logger.error("Erro no processamento do HTML: modalidade={}", modalidade, e);
                throw new RuntimeException("Falha no processamento dos dados de loteria", e);
            }
        });
    }

    private String extrairNumeroConcurso(String html) {
        Matcher matcher = PATTERN_CONCURSO.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private LocalDate extrairDataSorteio(String html) {
        Matcher matcher = PATTERN_DATA.matcher(html);
        if (matcher.find()) {
            try {
                int dia = Integer.parseInt(matcher.group(1));
                int mes = Integer.parseInt(matcher.group(2));
                int ano = Integer.parseInt(matcher.group(3));
                return LocalDate.of(ano, mes, dia);
            } catch (Exception e) {
                logger.warn("Erro ao converter data do sorteio", e);
            }
        }
        return null;
    }

    private java.util.List<String> extrairNumerosSorteados(String html, String modalidade) {
        java.util.List<String> numeros = new java.util.ArrayList<>();
        
        // Estratégia simplificada - buscar todos os números de 2 dígitos
        Matcher matcher = PATTERN_NUMEROS.matcher(html);
        int maxNumeros = obterMaximoNumeros(modalidade);
        
        while (matcher.find() && numeros.size() < maxNumeros) {
            String numero = matcher.group();
            if (isNumeroValido(numero, modalidade)) {
                numeros.add(numero);
            }
        }
        
        return numeros;
    }

    private void processarModalidadeEspecifica(String modalidade, String html, Map<String, Object> resultado) {
        switch (modalidade.toLowerCase()) {
            case "megasena" -> {
                // Extrair informações específicas da Mega Sena
                resultado.put("acumulou", html.contains("ACUMULOU"));
                resultado.put("premioEstimado", extrairPremio(html));
            }
            case "quina" -> {
                // Extrair informações específicas da Quina
                resultado.put("ganhadores", extrairGanhadores(html));
            }
            // Adicionar outros casos conforme necessário
        }
    }

    private boolean isNumeroValido(String numero, String modalidade) {
        try {
            int num = Integer.parseInt(numero);
            return switch (modalidade.toLowerCase()) {
                case "megasena" -> num >= 1 && num <= 60;
                case "quina" -> num >= 1 && num <= 80;
                case "lotofacil" -> num >= 1 && num <= 25;
                default -> num >= 1 && num <= 99;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int obterMaximoNumeros(String modalidade) {
        return switch (modalidade.toLowerCase()) {
            case "megasena" -> 6;
            case "quina" -> 5;
            case "lotofacil" -> 15;
            case "lotomania" -> 20;
            default -> 6;
        };
    }

    private String extrairPremio(String html) {
        // Implementar extração do valor do prêmio
        Pattern premioPattern = Pattern.compile("R\\$\\s*([\\d.,]+)");
        Matcher matcher = premioPattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Integer extrairGanhadores(String html) {
        // Implementar extração do número de ganhadores
        Pattern ganhadoresPattern = Pattern.compile("(\\d+)\\s+ganhadores?");
        Matcher matcher = ganhadoresPattern.matcher(html);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Erro ao converter número de ganhadores", e);
            }
        }
        return 0;
    }
}