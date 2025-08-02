# Aeron Low-Latency Monitoring Setup

## Overview

This monitoring solution provides **microsecond-level latency tracking** for the Aeron-based messaging system using Prometheus and Grafana. It captures comprehensive metrics across the entire data pipeline from external APIs to WebSocket clients.

## Architecture

```
[Finnhub WebSocket] → [Producer] → [Aeron UDP] → [Consumer] → [WebSocket Clients]
        ↓                ↓              ↓              ↓              ↓
    [Metrics]        [Metrics]    [Transport]    [Metrics]      [Metrics]
        ↓                ↓              ↓              ↓              ↓
                        [Prometheus] ← [100ms scrape] ← [Actuator Endpoints]
                             ↓
                        [Grafana Dashboard]
```

## Key Metrics Tracked

### Producer Metrics
- **Data Source Latency**: Time from API call to data received
- **Serialization Latency**: JSON serialization time
- **Aeron Publication Latency**: Time to publish to Aeron
- **End-to-End Latency**: Complete producer pipeline timing
- **Message Counts**: Published messages, errors, API calls
- **WebSocket Reconnections**: Connection stability tracking

### Consumer Metrics
- **Aeron Receive Latency**: Message reception from UDP
- **Deserialization Latency**: JSON parsing time
- **Processing Latency**: Complete message processing time
- **WebSocket Broadcast Latency**: Client delivery time
- **End-to-End Latency**: Producer timestamp to consumer completion
- **Queue Metrics**: Message queue depth and processing rates
- **Connection Metrics**: Active WebSocket clients

### System Metrics
- **Throughput**: Messages per second
- **Error Rates**: Failed operations per second
- **Resource Usage**: CPU, memory, network via node-exporter

## Quick Start

### 1. Start Monitoring Stack
```bash
# From the aeron-producer directory
docker-compose -f docker-compose.monitoring.yml up -d

# Verify services are running
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. Start Applications
```bash
# Terminal 1: Start Producer
cd /Users/yyordanov/dev/claude-training/aeron-producer
mvn spring-boot:run

# Terminal 2: Start Consumer
cd /Users/yyordanov/dev/claude-training/aeron-consumer
mvn spring-boot:run
```

### 3. Access Dashboards
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Producer Metrics**: http://localhost:8080/actuator/prometheus
- **Consumer Metrics**: http://localhost:8081/actuator/prometheus

## Dashboard Features

### Real-Time Latency Monitoring
- **Ultra-Fast Refresh**: 1-second dashboard updates
- **Microsecond Precision**: All latency metrics in microseconds
- **Color-Coded Thresholds**: Visual alerts for performance degradation

### Key Panels

#### 1. End-to-End Latency
- Tracks complete message journey from API to client
- **Target**: < 1000μs (1ms)
- **Alert Threshold**: > 1000μs

#### 2. Aeron Transport Latency
- Publication and reception times for Aeron messaging
- **Target**: < 100μs
- **Alert Threshold**: > 500μs

#### 3. Serialization Performance
- JSON serialization/deserialization timing
- Identifies bottlenecks in data transformation

#### 4. WebSocket Broadcast Latency
- Client delivery performance monitoring
- Tracks connection stability

#### 5. Throughput Metrics
- Messages per second rates
- Production vs consumption balance

#### 6. Error Monitoring
- Publication failures
- Processing errors
- WebSocket delivery failures

#### 7. Connection Status
- Active WebSocket clients
- Message queue depth

## Performance Targets

### Latency SLAs
- **End-to-End**: < 1000μs (1ms)
- **Aeron Publication**: < 100μs
- **Aeron Reception**: < 50μs
- **Serialization**: < 10μs
- **WebSocket Broadcast**: < 200μs

### Throughput Targets
- **Message Rate**: 1000+ messages/second
- **Error Rate**: < 0.1%
- **Availability**: > 99.9%

## Alerting Rules

### Critical Alerts
- **High End-to-End Latency**: > 1000μs for 5 seconds
- **Message Processing Errors**: > 0.1 errors/sec for 10 seconds
- **Publication Errors**: > 0.1 errors/sec for 10 seconds

### Warning Alerts
- **High Aeron Publication Latency**: > 500μs for 5 seconds
- **High Message Queue Depth**: > 100 messages for 15 seconds
- **High WebSocket Errors**: > 0.05 errors/sec for 15 seconds

### Info Alerts
- **No Messages Received**: No activity for 30 seconds
- **No WebSocket Connections**: No active clients for 1 minute

## Configuration

### Prometheus Scraping
- **Frequency**: 100ms (ultra-high frequency)
- **Timeout**: 50ms
- **Endpoints**: 
  - Producer: `localhost:8080/actuator/prometheus`
  - Consumer: `localhost:8081/actuator/prometheus`

### Grafana Settings
- **Refresh Rate**: 1 second
- **Time Range**: Last 5 minutes (adjustable)
- **Data Source**: Prometheus with 1s time interval

## Troubleshooting

### Common Issues

#### 1. No Metrics Appearing
```bash
# Check application endpoints
curl http://localhost:8080/actuator/prometheus | grep aeron
curl http://localhost:8081/actuator/prometheus | grep aeron

# Verify Prometheus targets
curl http://localhost:9090/api/v1/targets
```

#### 2. High Latency Alerts
- Check system resource usage
- Verify network connectivity
- Review application logs for errors
- Check Java GC activity

#### 3. Missing Data Points
- Verify applications are sending data
- Check Prometheus scrape errors
- Ensure time synchronization

### Performance Tuning

#### Application Level
```yaml
# Increase logging frequency for better visibility
logging:
  level:
    com.example.aeronproducer.metrics: DEBUG
    com.example.aeronconsumer.metrics: DEBUG
```

#### Prometheus Level
```yaml
# Increase retention for historical analysis
storage.tsdb.retention.time: 30d

# Optimize for high-frequency scraping
query.max-concurrency: 20
```

#### Grafana Level
```yaml
# Reduce query timeout for faster updates
query_timeout: 30s

# Increase max data points for detailed graphs
max_data_points: 1000
```

## Advanced Monitoring

### Custom Metrics
Add application-specific metrics:
```java
@Component
public class CustomMetrics {
    
    private final Timer customLatency;
    
    public CustomMetrics(MeterRegistry registry) {
        this.customLatency = Timer.builder("custom.latency")
            .description("Custom latency measurement")
            .register(registry);
    }
}
```

### Integration with APM Tools
- **Jaeger**: Distributed tracing integration
- **New Relic**: Application performance monitoring
- **DataDog**: Infrastructure and application monitoring

### Load Testing
```bash
# Generate test load for latency validation
curl -X POST http://localhost:8080/api/producer/test-load \
  -H "Content-Type: application/json" \
  -d '{"messageCount": 1000, "intervalMs": 1}'
```

## Production Deployment

### Security Considerations
- Configure Grafana authentication
- Secure Prometheus endpoints
- Use HTTPS for external access
- Network isolation for monitoring stack

### Scaling
- **Horizontal**: Multiple Prometheus instances for high availability
- **Vertical**: Increase resources for data retention
- **Federation**: Multi-region monitoring setup

### Backup & Recovery
```bash
# Backup Grafana dashboards
docker exec aeron-grafana grafana-cli admin export-dashboard aeron-latency-001

# Backup Prometheus data
docker run --rm -v prometheus_data:/data alpine tar czf - /data
```

## Integration Examples

### CI/CD Pipeline
```yaml
# Performance test integration
performance_test:
  script:
    - docker-compose -f docker-compose.monitoring.yml up -d
    - mvn test -Dtest=LatencyPerformanceTest
    - ./scripts/validate-latency-sla.sh
```

### Automated Alerting
```yaml
# Slack integration
route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'slack-notifications'
```

This monitoring setup provides production-ready observability for ultra-low latency messaging systems with Aeron.