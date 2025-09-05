package br.tec.facilitaservicos.scheduler.infraestrutura.configuracao;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * ============================================================================
 * ⚙️ CONFIGURAÇÃO DO QUARTZ SCHEDULER
 * ============================================================================
 * 
 * Configuração avançada do Quartz Scheduler para execução distribuída
 * de jobs com persistência em MySQL.
 * 
 * 🎯 FUNCIONALIDADES:
 * - Cluster distribuído
 * - Persistência em MySQL
 * - Thread pool configurável
 * - Misfire handling
 * - Spring integration
 * - Health checks
 * 
 * 📊 CARACTERÍSTICAS:
 * - Alta disponibilidade
 * - Load balancing automático
 * - Failover transparente
 * - Monitoramento via JMX
 * - Métricas detalhadas
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
@ConditionalOnProperty(name = "scheduler.quartz.enabled", havingValue = "true", matchIfMissing = true)
public class QuartzConfiguration {

    @Value("${scheduler.quartz.instance-name:ConexaoSorteScheduler}")
    private String instanceName;
    
    @Value("${scheduler.quartz.instance-id:AUTO}")
    private String instanceId;
    
    @Value("${scheduler.quartz.cluster.enabled:true}")
    private boolean clusterEnabled;
    
    @Value("${scheduler.quartz.cluster.checkin-interval:20000}")
    private int clusterCheckinInterval;
    
    @Value("${scheduler.quartz.thread-pool.count:10}")
    private int threadCount;
    
    @Value("${scheduler.quartz.thread-pool.priority:5}")
    private int threadPriority;
    
    @Value("${scheduler.quartz.misfire.threshold:60000}")
    private int misfireThreshold;
    
    @Value("${scheduler.quartz.job-store.table-prefix:qrtz_}")
    private String tablePrefix;

    /**
     * Factory personalizada para integração com Spring
     */
    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {
        private ApplicationContext applicationContext;

        public AutowiringSpringBeanJobFactory(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
            Object job = super.createJobInstance(bundle);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
            return job;
        }
    }

    /**
     * Configuração principal do Scheduler
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, ApplicationContext applicationContext) {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        
        // Configurar data source para persistência
        schedulerFactory.setDataSource(dataSource);
        
        // Job factory com autowiring
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory(applicationContext);
        schedulerFactory.setJobFactory(jobFactory);
        
        // Configurações customizadas
        schedulerFactory.setQuartzProperties(quartzProperties());
        
        // Nome da aplicação
        schedulerFactory.setApplicationContextSchedulerContextKey("applicationContext");
        
        // Aguardar jobs terminarem no shutdown
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
        
        // Timeout para shutdown
        schedulerFactory.setStartupDelay(10);
        
        // Auto start
        schedulerFactory.setAutoStartup(true);
        
        return schedulerFactory;
    }

    /**
     * Propriedades específicas do Quartz
     */
    private Properties quartzProperties() {
        Properties props = new Properties();
        
        // === CONFIGURAÇÕES BÁSICAS ===
        props.put("org.quartz.scheduler.instanceName", instanceName);
        props.put("org.quartz.scheduler.instanceId", instanceId);
        props.put("org.quartz.scheduler.skipUpdateCheck", "true");
        props.put("org.quartz.scheduler.jmx.export", "true");
        
        // === CONFIGURAÇÕES DE CLUSTER ===
        if (clusterEnabled) {
            props.put("org.quartz.jobStore.isClustered", "true");
            props.put("org.quartz.jobStore.clusterCheckinInterval", String.valueOf(clusterCheckinInterval));
            props.put("org.quartz.scheduler.instanceIdGenerator.class", "org.quartz.simpl.HostnameInstanceIdGenerator");
        } else {
            props.put("org.quartz.jobStore.isClustered", "false");
        }
        
        // === CONFIGURAÇÕES DE JOB STORE (MySQL) ===
        props.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        props.put("org.quartz.jobStore.useProperties", "false");
        props.put("org.quartz.jobStore.dataSource", "quartzDataSource");
        props.put("org.quartz.jobStore.tablePrefix", tablePrefix);
        props.put("org.quartz.jobStore.misfireThreshold", String.valueOf(misfireThreshold));
        props.put("org.quartz.jobStore.acquireTriggersWithinLock", "true");
        
        // === CONFIGURAÇÕES DE THREAD POOL ===
        props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.put("org.quartz.threadPool.threadCount", String.valueOf(threadCount));
        props.put("org.quartz.threadPool.threadPriority", String.valueOf(threadPriority));
        props.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        
        // === CONFIGURAÇÕES DE DATA SOURCE ===
        // O DataSource será injetado automaticamente pelo Spring
        
        // === CONFIGURAÇÕES DE PLUGINS ===
        // Plugin de logging
        props.put("org.quartz.plugin.triggHistory.class", "org.quartz.plugins.history.LoggingTriggerHistoryPlugin");
        props.put("org.quartz.plugin.triggHistory.triggerFiredMessage", 
            "Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss MM/dd/yyyy}");
        props.put("org.quartz.plugin.triggHistory.triggerCompleteMessage", 
            "Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss MM/dd/yyyy} with result: {9}");
        
        // Plugin de shutdown hook
        props.put("org.quartz.plugin.shutdownhook.class", "org.quartz.plugins.management.ShutdownHookPlugin");
        props.put("org.quartz.plugin.shutdownhook.cleanShutdown", "true");
        
        return props;
    }

    /**
     * Scheduler bean principal
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        
        // Configurações adicionais se necessário
        scheduler.getListenerManager().addSchedulerListener(new QuartzSchedulerListener());
        
        return scheduler;
    }

    /**
     * Factory alternativa usando StdSchedulerFactory
     */
    @Bean
    public org.quartz.SchedulerFactory quartzSchedulerFactory() {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        try {
            factory.initialize(quartzProperties());
        } catch (SchedulerException e) {
            throw new RuntimeException("Erro ao inicializar Quartz SchedulerFactory", e);
        }
        return factory;
    }

    /**
     * Listener para monitoramento do scheduler
     */
    public static class QuartzSchedulerListener implements org.quartz.SchedulerListener {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuartzSchedulerListener.class);
        
        @Override
        public void jobScheduled(org.quartz.Trigger trigger) {
            log.debug("📅 Job agendado: {} no grupo: {}", trigger.getJobKey().getName(), trigger.getJobKey().getGroup());
        }
        
        @Override
        public void jobUnscheduled(org.quartz.TriggerKey triggerKey) {
            log.debug("❌ Job desagendado: {}", triggerKey);
        }
        
        @Override
        public void triggerFinalized(org.quartz.Trigger trigger) {
            log.debug("🏁 Trigger finalizado: {}", trigger.getKey());
        }
        
        @Override
        public void triggerPaused(org.quartz.TriggerKey triggerKey) {
            log.debug("⏸️ Trigger pausado: {}", triggerKey);
        }
        
        @Override
        public void triggersPaused(String triggerGroup) {
            log.debug("⏸️ Triggers pausados no grupo: {}", triggerGroup);
        }
        
        @Override
        public void triggerResumed(org.quartz.TriggerKey triggerKey) {
            log.debug("▶️ Trigger retomado: {}", triggerKey);
        }
        
        @Override
        public void triggersResumed(String triggerGroup) {
            log.debug("▶️ Triggers retomados no grupo: {}", triggerGroup);
        }
        
        @Override
        public void jobAdded(org.quartz.JobDetail jobDetail) {
            log.debug("➕ Job adicionado: {} no grupo: {}", 
                jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
        }
        
        @Override
        public void jobDeleted(org.quartz.JobKey jobKey) {
            log.debug("🗑️ Job removido: {} do grupo: {}", jobKey.getName(), jobKey.getGroup());
        }
        
        @Override
        public void jobPaused(org.quartz.JobKey jobKey) {
            log.debug("⏸️ Job pausado: {} no grupo: {}", jobKey.getName(), jobKey.getGroup());
        }
        
        @Override
        public void jobsPaused(String jobGroup) {
            log.debug("⏸️ Jobs pausados no grupo: {}", jobGroup);
        }
        
        @Override
        public void jobResumed(org.quartz.JobKey jobKey) {
            log.debug("▶️ Job retomado: {} no grupo: {}", jobKey.getName(), jobKey.getGroup());
        }
        
        @Override
        public void jobsResumed(String jobGroup) {
            log.debug("▶️ Jobs retomados no grupo: {}", jobGroup);
        }
        
        @Override
        public void schedulerError(String msg, SchedulerException cause) {
            log.error("💥 Erro no scheduler: {} - {}", msg, cause.getMessage(), cause);
        }
        
        @Override
        public void schedulerInStandbyMode() {
            log.info("⏳ Scheduler em modo standby");
        }
        
        @Override
        public void schedulerStarted() {
            log.info("🚀 Scheduler iniciado com sucesso!");
        }
        
        @Override
        public void schedulerStarting() {
            log.info("🔄 Iniciando scheduler...");
        }
        
        @Override
        public void schedulerShutdown() {
            log.info("🛑 Scheduler finalizado");
        }
        
        @Override
        public void schedulerShuttingdown() {
            log.info("🔄 Finalizando scheduler...");
        }
        
        @Override
        public void schedulingDataCleared() {
            log.info("🧹 Dados de agendamento limpos");
        }
    }
}