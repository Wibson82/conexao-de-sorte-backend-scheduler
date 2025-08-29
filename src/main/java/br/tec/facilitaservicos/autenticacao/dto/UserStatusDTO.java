package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

/**
 * DTO para status online/offline do usuário.
 * 
 * Usado principalmente pelos microserviços de chat e notificações
 * para verificar disponibilidade do usuário.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Status de presença do usuário")
public class UserStatusDTO {
    
    @Schema(description = "ID do usuário", example = "123")
    private Long userId;
    
    @Schema(description = "Indica se o usuário está online", example = "true")
    private boolean online;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    @Schema(description = "Último acesso do usuário", example = "2025-08-28T14:30:00.000Z")
    private Instant lastSeen;
    
    @Schema(description = "Status textual do usuário", example = "ONLINE", 
            allowableValues = {"ONLINE", "OFFLINE", "AWAY", "BUSY", "INVISIBLE"})
    private String status;
    
    @Schema(description = "Mensagem de status personalizada", example = "Trabalhando")
    private String statusMessage;
    
    @Schema(description = "Dispositivo usado no último acesso", example = "WEB")
    private String lastDevice;
    
    @Schema(description = "IP do último acesso", example = "192.168.1.1")
    private String lastIpAddress;
    
    // Constructors
    public UserStatusDTO() {}
    
    public UserStatusDTO(Long userId, boolean online, Instant lastSeen, String status, 
                        String statusMessage, String lastDevice, String lastIpAddress) {
        this.userId = userId;
        this.online = online;
        this.lastSeen = lastSeen;
        this.status = status;
        this.statusMessage = statusMessage;
        this.lastDevice = lastDevice;
        this.lastIpAddress = lastIpAddress;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    
    public String getLastDevice() { return lastDevice; }
    public void setLastDevice(String lastDevice) { this.lastDevice = lastDevice; }
    
    public String getLastIpAddress() { return lastIpAddress; }
    public void setLastIpAddress(String lastIpAddress) { this.lastIpAddress = lastIpAddress; }
    
    // Builder Pattern - Manual Implementation (NO LOMBOK)
    public static UserStatusDTOBuilder builder() {
        return new UserStatusDTOBuilder();
    }
    
    public static class UserStatusDTOBuilder {
        private Long userId;
        private boolean online;
        private Instant lastSeen;
        private String status;
        private String statusMessage;
        private String lastDevice;
        private String lastIpAddress;
        
        public UserStatusDTOBuilder userId(Long userId) { this.userId = userId; return this; }
        public UserStatusDTOBuilder online(boolean online) { this.online = online; return this; }
        public UserStatusDTOBuilder lastSeen(Instant lastSeen) { this.lastSeen = lastSeen; return this; }
        public UserStatusDTOBuilder status(String status) { this.status = status; return this; }
        public UserStatusDTOBuilder statusMessage(String statusMessage) { this.statusMessage = statusMessage; return this; }
        public UserStatusDTOBuilder lastDevice(String lastDevice) { this.lastDevice = lastDevice; return this; }
        public UserStatusDTOBuilder lastIpAddress(String lastIpAddress) { this.lastIpAddress = lastIpAddress; return this; }
        
        public UserStatusDTO build() {
            return new UserStatusDTO(userId, online, lastSeen, status, statusMessage, lastDevice, lastIpAddress);
        }
    }
    
    /**
     * Cria um status online para o usuário.
     */
    public static UserStatusDTO online(Long userId) {
        return UserStatusDTO.builder()
            .userId(userId)
            .online(true)
            .lastSeen(Instant.now())
            .status("ONLINE")
            .build();
    }
    
    /**
     * Cria um status online com informações adicionais.
     */
    public static UserStatusDTO online(Long userId, String statusMessage, String device, String ipAddress) {
        return UserStatusDTO.builder()
            .userId(userId)
            .online(true)
            .lastSeen(Instant.now())
            .status("ONLINE")
            .statusMessage(statusMessage)
            .lastDevice(device)
            .lastIpAddress(ipAddress)
            .build();
    }
    
    /**
     * Cria um status offline para o usuário.
     */
    public static UserStatusDTO offline(Long userId, Instant lastSeen) {
        return UserStatusDTO.builder()
            .userId(userId)
            .online(false)
            .lastSeen(lastSeen)
            .status("OFFLINE")
            .build();
    }
    
    /**
     * Cria um status away (ausente) para o usuário.
     */
    public static UserStatusDTO away(Long userId, Instant lastSeen) {
        return UserStatusDTO.builder()
            .userId(userId)
            .online(true)
            .lastSeen(lastSeen)
            .status("AWAY")
            .build();
    }
    
    /**
     * Cria um status busy (ocupado) para o usuário.
     */
    public static UserStatusDTO busy(Long userId, String statusMessage) {
        return UserStatusDTO.builder()
            .userId(userId)
            .online(true)
            .lastSeen(Instant.now())
            .status("BUSY")
            .statusMessage(statusMessage)
            .build();
    }
    
    /**
     * Verifica se o usuário está disponível para receber mensagens.
     */
    public boolean isAvailable() {
        return online && ("ONLINE".equals(status) || "AWAY".equals(status));
    }
    
    /**
     * Verifica se o usuário está ocupado.
     */
    public boolean isBusy() {
        return "BUSY".equals(status);
    }
    
    /**
     * Verifica se o usuário foi visto recentemente (últimos 5 minutos).
     */
    public boolean wasRecentlyActive() {
        if (lastSeen == null) {
            return false;
        }
        
        Instant fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(5));
        return lastSeen.isAfter(fiveMinutesAgo);
    }
    
    /**
     * Verifica se o usuário foi visto hoje.
     */
    public boolean wasActiveToday() {
        if (lastSeen == null) {
            return false;
        }
        
        Instant todayStart = Instant.now().minus(Duration.ofHours(24));
        return lastSeen.isAfter(todayStart);
    }
    
    /**
     * Retorna há quanto tempo o usuário foi visto pela última vez.
     */
    public Duration timeSinceLastSeen() {
        if (lastSeen == null) {
            return Duration.ofDays(365); // Retorna 1 ano se nunca foi visto
        }
        
        return Duration.between(lastSeen, Instant.now());
    }
    
    /**
     * Retorna uma representação amigável do tempo desde a última atividade.
     */
    public String getLastSeenFormatted() {
        if (online) {
            return "Online agora";
        }
        
        if (lastSeen == null) {
            return "Nunca visto";
        }
        
        Duration timeSince = timeSinceLastSeen();
        
        if (timeSince.toMinutes() < 1) {
            return "Há alguns segundos";
        } else if (timeSince.toMinutes() < 60) {
            return String.format("Há %d minutos", timeSince.toMinutes());
        } else if (timeSince.toHours() < 24) {
            return String.format("Há %d horas", timeSince.toHours());
        } else {
            return String.format("Há %d dias", timeSince.toDays());
        }
    }
    
    /**
     * Retorna informações para logging (sem dados sensíveis).
     */
    public String toLogString() {
        return String.format("UserStatus[userId=%s, online=%s, status=%s, lastSeen=%s]", 
                           userId, online, status, lastSeen);
    }
}