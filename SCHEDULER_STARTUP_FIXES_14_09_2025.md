# Correção de Erros de Inicialização do Microserviço Scheduler

Data: 14 de setembro de 2025

## Problema Resolvido

Foi identificado um erro grave de inicialização no microserviço Scheduler que estava causando um loop infinito de reinicialização no servidor de produção. O erro específico era:

```
org.springframework.context.annotation.ConflictingBeanDefinitionException:
Annotation-specified bean name 'jobController' for bean class [br.tec.facilitaservicos.scheduler.infraestrutura.web.JobController]
conflicts with existing, non-compatible bean definition of same name and class [br.tec.facilitaservicos.scheduler.apresentacao.controlador.JobController]
```

O container estava tentando reiniciar indefinidamente ao encontrar este erro, causando uso excessivo de recursos no servidor e logs inflados.

## Solução Implementada

### 1. Correção do Conflito de Beans
- Renomeamos o controlador `JobController` na camada de apresentação para `LoteriaJobController`
- Marcamos o controlador antigo como obsoleto para remoção segura futura
- Mantivemos a mesma funcionalidade nas APIs

### 2. Implementação de Retry Limitado
- Criado script robusto de entrada para container (`healthcheck-entrypoint.sh`)
- Implementadas tentativas limitadas de inicialização (máximo de 10)
- Adicionado intervalo de 30 segundos entre tentativas
- Logs detalhados para diagnóstico de problemas

### 3. Melhoria no Healthcheck
- Verificação de conexão com o banco de dados antes de iniciar a aplicação
- Suporte a múltiplos hosts para conexão com banco (conexao-mysql, scheduler-mysql, etc.)
- Detecção automática de gateway e localhost

## Arquivos Modificados

1. `Dockerfile`
   - Substituído script de entrada inline por arquivo externo
   - Atualizado ENTRYPOINT para ambos modos normal e debug

2. `docker/healthcheck-entrypoint.sh` (novo arquivo)
   - Script robusto para inicialização com retry e healthcheck
   - Limite de 10 tentativas com intervalo de 30 segundos

3. `src/main/java/br/tec/facilitaservicos/scheduler/apresentacao/controlador/JobController.java`
   - Renomeado para `JobController_DEPRECATED` e removido @RestController

4. `src/main/java/br/tec/facilitaservicos/scheduler/apresentacao/controlador/LoteriaJobController.java` (novo arquivo)
   - Nova implementação do controlador sem conflito de nomes

## Como Testar

1. Construa a imagem Docker com as correções:
   ```bash
   docker build -t conexaodesorte/scheduler:latest .
   ```

2. Execute o container:
   ```bash
   docker run -p 8084:8084 conexaodesorte/scheduler:latest
   ```

3. Verifique os logs para confirmar a inicialização correta:
   ```bash
   docker logs -f <container_id>
   ```

## Próximos Passos

1. Remover completamente o controlador obsoleto após período de observação
2. Adicionar testes unitários para o novo controlador
3. Considerar adicionar estratégia de circuit breaker para conexões com serviços externos

## Informações de Contato

Para quaisquer dúvidas sobre essas alterações, entre em contato com a equipe técnica.