package com.example.aeronproducer.config;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aeron configuration class that sets up high-performance messaging infrastructure.
 * Configures MediaDriver for low-latency communication with dedicated threading mode
 * and optimized idle strategies for production workloads.
 */
@Configuration
@EnableConfigurationProperties(AeronProperties.class)
public class AeronConfig {

    private static final Logger logger = LoggerFactory.getLogger(AeronConfig.class);
    
    private final AeronProperties aeronProperties;

    public AeronConfig(AeronProperties aeronProperties) {
        this.aeronProperties = aeronProperties;
    }

    /**
     * Creates and configures the MediaDriver bean for Aeron messaging.
     * MediaDriver handles the low-level transport and manages network resources.
     * 
     * @return configured MediaDriver instance with production settings
     */
    @Bean
    public MediaDriver mediaDriver() {
        AeronProperties.MediaDriver config = aeronProperties.getMediaDriver();
        
        // Configure idle strategies for different thread types
        // BackoffIdleStrategy for conductor: balances CPU usage with responsiveness
        IdleStrategy conductorIdleStrategy = new BackoffIdleStrategy(
            config.getConductorIdleStrategyMaxSpins(), 10, 1, 1);
        // BusySpinIdleStrategy for sender/receiver: maximizes throughput and minimizes latency
        IdleStrategy senderIdleStrategy = new BusySpinIdleStrategy();
        IdleStrategy receiverIdleStrategy = new BusySpinIdleStrategy();

        MediaDriver.Context context = new MediaDriver.Context()
                // Directory for Aeron's memory-mapped files and control structures
                .aeronDirectoryName(config.getDir())
                // DEDICATED mode: separate threads for conductor, sender, and receiver for maximum performance
                .threadingMode(ThreadingMode.DEDICATED)
                // Assign idle strategies to respective threads
                .conductorIdleStrategy(conductorIdleStrategy)
                .senderIdleStrategy(senderIdleStrategy)
                .receiverIdleStrategy(receiverIdleStrategy)
                // Term buffer size affects message throughput and memory usage
                .publicationTermBufferLength(config.getPublicationTermBufferLength())
                .ipcTermBufferLength(config.getIpcTermBufferLength())
                // Client timeout settings for connection management
                .clientLivenessTimeoutNs(config.getClientLivenessTimeoutNs())
                .publicationLingerTimeoutNs(config.getPublicationLingerTimeoutNs());

        logger.info("Starting MediaDriver with production configuration:");
        logger.info("  Directory: {}", config.getDir());
        logger.info("  Threading Mode: {}", ThreadingMode.DEDICATED);
        logger.info("  Term Buffer Length: {} bytes", config.getPublicationTermBufferLength());
        logger.info("  Socket Send Buffer: {} bytes", config.getSocketSndbufLength());
        logger.info("  Socket Receive Buffer: {} bytes", config.getSocketRcvbufLength());
        
        return MediaDriver.launchEmbedded(context);
    }

    /**
     * Creates and configures the Aeron client instance.
     * Aeron client manages publications and subscriptions, connecting to the MediaDriver.
     * 
     * @param mediaDriver the MediaDriver instance to connect to
     * @return configured Aeron client instance
     */
    @Bean
    public Aeron aeron(MediaDriver mediaDriver) {
        Aeron.Context context = new Aeron.Context()
                // Connect to the same directory as the MediaDriver
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                // Keep-alive interval to maintain client-driver connection (1 second)
                .keepAliveIntervalNs(1_000_000_000L);
        
        logger.info("Connecting to Aeron with enhanced client settings");
        return Aeron.connect(context);
    }

    /**
     * Creates and configures a Publication for sending messages.
     * Publication represents a stream endpoint for message transmission.
     * Waits for the publication to be connected before returning.
     * 
     * @param aeron the Aeron client instance
     * @return connected Publication ready for message sending
     */
    @Bean
    public Publication publication(Aeron aeron) {
        AeronProperties.Publication config = aeronProperties.getPublication();
        
        logger.info("Creating publication: channel={}, streamId={}", 
                   config.getChannel(), config.getStreamId());
        
        Publication publication = aeron.addPublication(config.getChannel(), config.getStreamId());
        
        // Wait for publication to be connected to ensure it's ready for message sending
        while (!publication.isConnected()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for publication connection", e);
            }
        }
        
        logger.info("Publication connected successfully");
        return publication;
    }
}