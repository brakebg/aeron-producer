# Aeron Producer Application

## Overview

The Aeron Producer is a Spring Boot application that streams real-time financial data using the Aeron messaging library for ultra-low latency communication. It fetches live stock data from external APIs and publishes it via Aeron's UDP transport to connected consumers.

## Architecture

### Core Components

#### 1. **MediaDriver Configuration** (`AeronConfig.java`)
- **Production-Ready Setup**: Dedicated threading mode with optimized idle strategies
- **Performance Tuning**: 2MB term buffers, enhanced client timeouts
- **Monitoring**: Comprehensive logging and metrics collection

#### 2. **Stock Data Sources**
- **Alpha Vantage REST API**: Fallback polling mechanism (25 requests/day limit)
- **Finnhub WebSocket API**: Primary real-time streaming source
- **Dual Integration**: Automatic failover between data sources

#### 3. **Aeron Publishing**
- **Channel**: `aeron:udp?endpoint=localhost:40123`
- **Stream ID**: `1001`
- **Message Format**: JSON-serialized stock trade data

### Data Flow

```
[Finnhub WebSocket] → [StockDataService] → [Aeron Publication] → [Network UDP]
                                ↑
[Alpha Vantage REST] → [ScheduledPolling]
```

## Key Features

### Real-Time Data Streaming
- **WebSocket Subscription**: Live trade data from Finnhub
- **Automatic Reconnection**: Resilient connection handling
- **Rate Limiting**: Respects API limitations

### Configuration Management
- **Type-Safe Configuration**: `AeronProperties` with validation
- **Environment-Specific**: YAML-based configuration
- **Hot Reloading**: Spring Boot configuration processor support

### Monitoring & Observability
- **Actuator Endpoints**: Health, metrics, info
- **Prometheus Metrics**: Performance monitoring
- **Structured Logging**: Debug-level application logging

## API Integration

### Finnhub WebSocket
```java
// Real-time trade subscription
WebSocket connects to: wss://ws.finnhub.io?token={API_KEY}
Subscribe message: {"type":"subscribe","symbol":"AAPL"}
```

**Trade Message Format**:
```json
{
  "data": [{
    "s": "AAPL",      // Symbol
    "p": 185.50,      // Price
    "t": 1642781400,  // Timestamp
    "v": 100          // Volume
  }],
  "type": "trade"
}
```

### Alpha Vantage REST
```java
// Backup polling endpoint
GET https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={API_KEY}
```

## Configuration

### Application Properties (`application.yml`)

```yaml
# Aeron Configuration
aeron:
  media-driver:
    dir: /tmp/aeron-producer
    threading-mode: 3  # DEDICATED
    publication-term-buffer-length: 2097152  # 2MB
    client-liveness-timeout-ns: 10000000000  # 10 seconds
  publication:
    channel: aeron:udp?endpoint=localhost:40123
    stream-id: 1001

# Stock Data APIs
finnhub:
  api:
    key: ${FINNHUB_API_KEY}
  symbols: AAPL,MSFT,TSLA,GOOGL,AMZN
  websocket:
    enabled: true

alphavantage:
  api:
    key: ${ALPHAVANTAGE_API_KEY}
  stocks:
    symbols: AAPL,GOOGL,MSFT,TSLA,AMZN
    fetch-interval: 60000  # 1 minute
```

### Environment Variables
```bash
FINNHUB_API_KEY=your_finnhub_api_key
ALPHAVANTAGE_API_KEY=your_alphavantage_api_key
```

## Performance Characteristics

### Aeron MediaDriver Optimization
- **Threading Mode**: DEDICATED (separate threads for conductor, sender, receiver)
- **Idle Strategies**: 
  - Conductor: BackoffIdleStrategy (20 max spins)
  - Sender/Receiver: BusySpinIdleStrategy
- **Buffer Sizes**: 2MB term buffers for optimal throughput

### Latency Metrics
- **End-to-End**: Sub-millisecond from WebSocket to Aeron publication
- **Network Transport**: UDP with minimal OS overhead
- **Message Throughput**: Supports high-frequency trading data

## Development

### Prerequisites
- Java 21
- Maven 3.6+
- Valid API keys for Finnhub and Alpha Vantage

### Build & Run
```bash
# Compile
mvn clean compile

# Run application
mvn spring-boot:run

# Run with custom JVM options
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--add-opens java.base/sun.nio.ch=ALL-UNNAMED"
```

### Testing Data Flow
```bash
# Check application health
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics

# Monitor Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Error Handling

### WebSocket Reconnection
- **Automatic Retry**: Exponential backoff on connection failures
- **Fallback**: Switches to Alpha Vantage REST polling
- **Circuit Breaker**: Prevents cascade failures

### Aeron Publication Errors
- **Connection Monitoring**: Waits for publication connectivity
- **Backpressure Handling**: Manages slow consumer scenarios
- **Resource Cleanup**: Proper MediaDriver shutdown

## Troubleshooting

### Common Issues

1. **Module Access Errors (Java 17+)**
   ```bash
   Solution: Add JVM argument --add-opens java.base/sun.nio.ch=ALL-UNNAMED
   ```

2. **API Rate Limiting**
   ```bash
   Alpha Vantage: 25 requests/day limit
   Solution: Ensure Finnhub WebSocket is primary source
   ```

3. **Aeron Directory Conflicts**
   ```bash
   Error: Directory already in use
   Solution: Change aeron.media-driver.dir or stop conflicting processes
   ```

### Logging Configuration
```yaml
logging:
  level:
    com.example.aeronproducer: DEBUG
    io.aeron: INFO
```

## Integration Points

### Consumer Applications
- **Channel Compatibility**: Must subscribe to same UDP endpoint
- **Stream ID Matching**: Consumer stream-id must match (1001)
- **Message Format**: JSON deserialization required

### Monitoring Systems
- **Prometheus**: Scrape `/actuator/prometheus`
- **Health Checks**: Monitor `/actuator/health`
- **Custom Metrics**: Business-specific trading metrics

## Security Considerations

- **API Keys**: Store in environment variables, not in code
- **Network Security**: Consider firewall rules for UDP traffic
- **Resource Limits**: Monitor memory and CPU usage under load

## Future Enhancements

- **Multiple Data Sources**: Add Bloomberg, Reuters integration
- **Message Compression**: Implement efficient serialization
- **Clustering**: Multi-instance producer deployment
- **Security**: Add authentication and encryption layers