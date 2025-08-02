package com.example.aeronproducer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "aeron")
@Validated
public class AeronProperties {

    private MediaDriver mediaDriver = new MediaDriver();
    private Publication publication = new Publication();
    private Producer producer = new Producer();

    public MediaDriver getMediaDriver() {
        return mediaDriver;
    }

    public void setMediaDriver(MediaDriver mediaDriver) {
        this.mediaDriver = mediaDriver;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public static class MediaDriver {
        @NotBlank
        private String dir = "/tmp/aeron-producer";
        
        @Min(1)
        private int threadingMode = 3; // DEDICATED
        
        @Min(1)
        private long conductorIdleStrategyMaxSpins = 20L;
        
        @Min(1)
        private long senderIdleStrategyMaxSpins = 20L;
        
        @Min(1)
        private long receiverIdleStrategyMaxSpins = 20L;
        
        @Min(1024)
        private int termBufferSparseFileLength = 64 * 1024 * 1024; // 64MB
        
        @Min(1024)
        private int publicationTermBufferLength = 2 * 1024 * 1024; // 2MB
        
        @Min(1024)
        private int ipcTermBufferLength = 2 * 1024 * 1024; // 2MB
        
        @Min(1)
        private long clientLivenessTimeoutNs = 10_000_000_000L; // 10 seconds
        
        @Min(1)
        private long publicationLingerTimeoutNs = 5_000_000_000L; // 5 seconds
        
        @Min(1)
        private int socketSndbufLength = 2 * 1024 * 1024; // 2MB
        
        @Min(1)
        private int socketRcvbufLength = 2 * 1024 * 1024; // 2MB
        
        private boolean enableExperimentalFeatures = false;
        private boolean printConfigurationOnStart = true;
        private boolean warningsEnabled = true;

        // Getters and setters
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        
        public int getThreadingMode() { return threadingMode; }
        public void setThreadingMode(int threadingMode) { this.threadingMode = threadingMode; }
        
        public long getConductorIdleStrategyMaxSpins() { return conductorIdleStrategyMaxSpins; }
        public void setConductorIdleStrategyMaxSpins(long conductorIdleStrategyMaxSpins) { 
            this.conductorIdleStrategyMaxSpins = conductorIdleStrategyMaxSpins; 
        }
        
        public long getSenderIdleStrategyMaxSpins() { return senderIdleStrategyMaxSpins; }
        public void setSenderIdleStrategyMaxSpins(long senderIdleStrategyMaxSpins) { 
            this.senderIdleStrategyMaxSpins = senderIdleStrategyMaxSpins; 
        }
        
        public long getReceiverIdleStrategyMaxSpins() { return receiverIdleStrategyMaxSpins; }
        public void setReceiverIdleStrategyMaxSpins(long receiverIdleStrategyMaxSpins) { 
            this.receiverIdleStrategyMaxSpins = receiverIdleStrategyMaxSpins; 
        }
        
        public int getTermBufferSparseFileLength() { return termBufferSparseFileLength; }
        public void setTermBufferSparseFileLength(int termBufferSparseFileLength) { 
            this.termBufferSparseFileLength = termBufferSparseFileLength; 
        }
        
        public int getPublicationTermBufferLength() { return publicationTermBufferLength; }
        public void setPublicationTermBufferLength(int publicationTermBufferLength) { 
            this.publicationTermBufferLength = publicationTermBufferLength; 
        }
        
        public int getIpcTermBufferLength() { return ipcTermBufferLength; }
        public void setIpcTermBufferLength(int ipcTermBufferLength) { 
            this.ipcTermBufferLength = ipcTermBufferLength; 
        }
        
        public long getClientLivenessTimeoutNs() { return clientLivenessTimeoutNs; }
        public void setClientLivenessTimeoutNs(long clientLivenessTimeoutNs) { 
            this.clientLivenessTimeoutNs = clientLivenessTimeoutNs; 
        }
        
        public long getPublicationLingerTimeoutNs() { return publicationLingerTimeoutNs; }
        public void setPublicationLingerTimeoutNs(long publicationLingerTimeoutNs) { 
            this.publicationLingerTimeoutNs = publicationLingerTimeoutNs; 
        }
        
        public int getSocketSndbufLength() { return socketSndbufLength; }
        public void setSocketSndbufLength(int socketSndbufLength) { 
            this.socketSndbufLength = socketSndbufLength; 
        }
        
        public int getSocketRcvbufLength() { return socketRcvbufLength; }
        public void setSocketRcvbufLength(int socketRcvbufLength) { 
            this.socketRcvbufLength = socketRcvbufLength; 
        }
        
        public boolean isEnableExperimentalFeatures() { return enableExperimentalFeatures; }
        public void setEnableExperimentalFeatures(boolean enableExperimentalFeatures) { 
            this.enableExperimentalFeatures = enableExperimentalFeatures; 
        }
        
        public boolean isPrintConfigurationOnStart() { return printConfigurationOnStart; }
        public void setPrintConfigurationOnStart(boolean printConfigurationOnStart) { 
            this.printConfigurationOnStart = printConfigurationOnStart; 
        }
        
        public boolean isWarningsEnabled() { return warningsEnabled; }
        public void setWarningsEnabled(boolean warningsEnabled) { 
            this.warningsEnabled = warningsEnabled; 
        }
    }

    public static class Publication {
        @NotBlank
        private String channel = "aeron:udp?endpoint=localhost:40123";
        
        @NotNull
        @Min(1)
        private Integer streamId = 1001;

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        
        public Integer getStreamId() { return streamId; }
        public void setStreamId(Integer streamId) { this.streamId = streamId; }
    }

    public static class Producer {
        private boolean autoSend = false;
        private boolean stockData = true;

        public boolean isAutoSend() { return autoSend; }
        public void setAutoSend(boolean autoSend) { this.autoSend = autoSend; }
        
        public boolean isStockData() { return stockData; }
        public void setStockData(boolean stockData) { this.stockData = stockData; }
    }
}