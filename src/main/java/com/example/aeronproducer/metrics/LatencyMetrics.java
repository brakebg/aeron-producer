package com.example.aeronproducer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class LatencyMetrics {

    private final Timer dataSourceLatency;
    private final Timer serializationLatency;
    private final Timer aeronPublicationLatency;
    private final Timer endToEndLatency;
    
    private final Counter messagesPublished;
    private final Counter publicationErrors;
    private final Counter apiCallsTotal;
    private final Counter websocketReconnections;

    public LatencyMetrics(MeterRegistry meterRegistry) {
        // Latency timers (microsecond precision)
        this.dataSourceLatency = Timer.builder("aeron.producer.datasource.latency")
                .description("Time from API call to data received")
                .register(meterRegistry);
                
        this.serializationLatency = Timer.builder("aeron.producer.serialization.latency")
                .description("Time to serialize message to JSON")
                .register(meterRegistry);
                
        this.aeronPublicationLatency = Timer.builder("aeron.producer.publication.latency")
                .description("Time from serialize to Aeron publication success")
                .register(meterRegistry);
                
        this.endToEndLatency = Timer.builder("aeron.producer.end_to_end.latency")
                .description("Complete pipeline latency from API to Aeron publish")
                .register(meterRegistry);

        // Counters
        this.messagesPublished = Counter.builder("aeron.producer.messages.published")
                .description("Total messages published via Aeron")
                .register(meterRegistry);
                
        this.publicationErrors = Counter.builder("aeron.producer.publication.errors")
                .description("Failed Aeron publication attempts")
                .register(meterRegistry);
                
        this.apiCallsTotal = Counter.builder("aeron.producer.api.calls.total")
                .description("Total external API calls made")
                .register(meterRegistry);
                
        this.websocketReconnections = Counter.builder("aeron.producer.websocket.reconnections")
                .description("WebSocket reconnection attempts")
                .register(meterRegistry);
    }

    public Timer.Sample startDataSourceTimer() {
        return Timer.start();
    }

    public void recordDataSourceLatency(Timer.Sample sample) {
        sample.stop(dataSourceLatency);
    }

    public Timer.Sample startSerializationTimer() {
        return Timer.start();
    }

    public void recordSerializationLatency(Timer.Sample sample) {
        sample.stop(serializationLatency);
    }

    public Timer.Sample startPublicationTimer() {
        return Timer.start();
    }

    public void recordPublicationLatency(Timer.Sample sample) {
        sample.stop(aeronPublicationLatency);
    }

    public void recordEndToEndLatency(long startNanos) {
        long durationNanos = System.nanoTime() - startNanos;
        endToEndLatency.record(Duration.ofNanos(durationNanos));
    }

    public void incrementMessagesPublished() {
        messagesPublished.increment();
    }

    public void incrementPublicationErrors() {
        publicationErrors.increment();
    }

    public void incrementApiCalls() {
        apiCallsTotal.increment();
    }

    public void incrementWebSocketReconnections() {
        websocketReconnections.increment();
    }

    // Convenience method for measuring code blocks
    public <T> T timeDataSource(java.util.function.Supplier<T> supplier) {
        Timer.Sample sample = Timer.start();
        try {
            return supplier.get();
        } finally {
            sample.stop(dataSourceLatency);
        }
    }

    public <T> T timeSerialization(java.util.function.Supplier<T> supplier) {
        Timer.Sample sample = Timer.start();
        try {
            return supplier.get();
        } finally {
            sample.stop(serializationLatency);
        }
    }

    public <T> T timePublication(java.util.function.Supplier<T> supplier) {
        Timer.Sample sample = Timer.start();
        try {
            return supplier.get();
        } finally {
            sample.stop(aeronPublicationLatency);
        }
    }
}