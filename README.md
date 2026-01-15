# Distributed Document Search Service

Enterprise-grade distributed document search service capable of searching through millions of documents with sub-second response times. This service demonstrates enterprise architectural patterns including multi-tenancy, fault tolerance, and horizontal scalability.

## Features

- ✅ **Full-text Search**: Powered by Elasticsearch with relevance ranking
- ✅ **Multi-tenancy**: Tenant isolation with header-based tenant identification
- ✅ **Caching**: Multi-tier caching (Redis + application cache) for optimal performance
- ✅ **Rate Limiting**: Per-tenant rate limiting to prevent abuse
- ✅ **Asynchronous Indexing**: Message queue-based document indexing
- ✅ **Fault Tolerance**: Circuit breakers, retries, and graceful degradation
- ✅ **Health Monitoring**: Comprehensive health checks with dependency status
- ✅ **Horizontal Scalability**: Stateless design for easy scaling

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture documentation.

### High-Level Components

- **API Layer**: Spring Boot REST API
- **Search Engine**: Elasticsearch 8.11
- **Database**: PostgreSQL 15 (metadata storage)
- **Cache**: Redis 7
- **Message Queue**: RabbitMQ 3 (asynchronous indexing)

## Prerequisites

- Java 17+
- Docker and Docker Compose
- Maven 3.9+ (optional, Docker handles build)

## Quick Start

### 1. Start All Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Elasticsearch (port 9200)
- Redis (port 6379)
- RabbitMQ (port 5672, Management UI: 15672)
- Application (port 8080)

### 2. Wait for Services to be Ready

Wait approximately 30-60 seconds for all services to start and be healthy. You can check the health endpoint:

```bash
curl http://localhost:8080/api/v1/health
```

### 3. Test the API

See [API_EXAMPLES.md](API_EXAMPLES.md) for detailed API examples.

**Create a document:**
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -d '{
    "title": "Introduction to Distributed Systems",
    "content": "Distributed systems are collections of independent computers that appear to users as a single coherent system.",
    "metadata": {
      "author": "John Doe",
      "category": "technical"
    }
  }'
```

**Search documents:**
```bash
curl "http://localhost:8080/api/v1/search?q=distributed&tenant=tenant-123&page=1&size=10"
```

**Get document:**
```bash
curl -X GET http://localhost:8080/api/v1/documents/{document-id} \
  -H "X-Tenant-Id: tenant-123"
```

## Local Development (Without Docker)

### 1. Start Dependencies

You'll need to run PostgreSQL, Elasticsearch, Redis, and RabbitMQ locally or use Docker Compose for just the dependencies:

```bash
docker-compose up -d postgresql elasticsearch redis rabbitmq
```

### 2. Build and Run

```bash
mvn clean package
java -jar target/document-search-1.0.0.jar
```

Or run directly with Maven:

```bash
mvn spring-boot:run
```

## API Endpoints

### POST /api/v1/documents
Index a new document.

**Headers:**
- `X-Tenant-Id` (required): Tenant identifier

**Request Body:**
```json
{
  "title": "Document Title",
  "content": "Document content...",
  "metadata": {
    "author": "John Doe",
    "category": "technical"
  }
}
```

### GET /api/v1/search
Search documents.

**Query Parameters:**
- `q` (required): Search query
- `tenant` (required): Tenant ID
- `page` (optional): Page number (default: 1)
- `size` (optional): Results per page (default: 10)

### GET /api/v1/documents/{id}
Retrieve document details.

**Headers:**
- `X-Tenant-Id` (required): Tenant identifier

### DELETE /api/v1/documents/{id}
Delete a document.

**Headers:**
- `X-Tenant-Id` (required): Tenant identifier

### GET /api/v1/health
Health check with dependency status.

## Configuration

Configuration is managed via `application.yml`. Key settings:

- **Cache TTL**: Search results (5 min), Documents (1 hour)
- **Rate Limiting**: 100 requests/minute per tenant (configurable)
- **Database**: PostgreSQL connection pool settings
- **Elasticsearch**: Connection timeout and socket timeout
- **Circuit Breakers**: Failure thresholds and retry policies

## Multi-Tenancy

The service supports multi-tenancy through:

1. **Tenant Identification**: Via `X-Tenant-Id` header or `tenant` query parameter
2. **Data Isolation**: All queries filtered by tenant ID
3. **Rate Limiting**: Per-tenant rate limits
4. **Cache Isolation**: Tenant ID included in cache keys

## Performance Considerations

- **Caching**: Search results cached for 5 minutes, documents for 1 hour
- **Asynchronous Indexing**: Documents indexed asynchronously via RabbitMQ
- **Connection Pooling**: Database and Redis connection pools configured
- **Circuit Breakers**: Prevent cascading failures
- **Retry Logic**: Automatic retries with exponential backoff

## Production Readiness

See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) for detailed production readiness analysis covering:

- Scalability strategies (100x growth)
- Resilience patterns (circuit breakers, retries, failover)
- Security (authentication, encryption, multi-tenancy)
- Observability (metrics, logging, tracing)
- Performance optimization
- Operations (deployment, backups, SLA)

## Testing

### Manual Testing

Use the examples in [API_EXAMPLES.md](API_EXAMPLES.md) or import the Postman collection.

### Unit Tests

```bash
mvn test
```

## Monitoring

### Health Check

```bash
curl http://localhost:8080/api/v1/health
```

### Actuator Endpoints

- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

### RabbitMQ Management UI

Access at: `http://localhost:15672`
- Username: `guest`
- Password: `guest`

### Elasticsearch

Check cluster health:
```bash
curl http://localhost:9200/_cluster/health
```

## Troubleshooting

### Services Not Starting

1. Check Docker logs: `docker-compose logs`
2. Verify ports are not in use: `netstat -an | grep <port>`
3. Check service health: `docker-compose ps`

### Application Not Connecting to Services

1. Verify services are running: `docker-compose ps`
2. Check application logs: `docker-compose logs app`
3. Verify network connectivity between containers

### Elasticsearch Index Issues

1. Check Elasticsearch logs: `docker-compose logs elasticsearch`
2. Verify index exists: `curl http://localhost:9200/_cat/indices`
3. Check mapping: `curl http://localhost:9200/documents_v1/_mapping`

## Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/distributed/documentsearch/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── listener/        # Message queue listeners
│   │   │   ├── model/            # Entity models
│   │   │   ├── repository/      # Data repositories
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       └── application.yml  # Configuration
│   └── test/                     # Test files
├── docker-compose.yml            # Docker Compose configuration
├── Dockerfile                    # Application Docker image
├── pom.xml                       # Maven dependencies
├── ARCHITECTURE.md              # Architecture documentation
├── PRODUCTION_READINESS.md      # Production analysis
├── EXPERIENCE_SHOWCASE.md       # Experience examples
└── API_EXAMPLES.md              # API usage examples
```

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Search**: Elasticsearch 8.11.0
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Message Queue**: RabbitMQ 3
- **Resilience**: Resilience4j
- **Build Tool**: Maven

## Limitations & Future Enhancements

### Current Limitations (Prototype)

- Basic authentication/authorization (can be extended)
- Single Elasticsearch node (production would use cluster)
- Simple rate limiting (can be enhanced with sliding window)
- Basic search features (can add fuzzy search, faceted search, highlighting)

### Future Enhancements

- Advanced search features (fuzzy search, faceted search, autocomplete)
- Authentication/Authorization (JWT, OAuth2)
- Advanced analytics and reporting
- Document versioning
- Bulk import/export
- Advanced monitoring and alerting
- Blue-green deployment automation

## Contributing

This is a technical assessment project. For production use, consider:

1. Adding comprehensive unit and integration tests
2. Implementing proper authentication/authorization
3. Adding API documentation (OpenAPI/Swagger)
4. Setting up CI/CD pipeline
5. Adding performance benchmarks
6. Implementing advanced search features

## License

This project is created for technical assessment purposes.

## Contact

For questions or issues, please refer to the architecture and production readiness documentation.

---

**Note**: This is a prototype demonstrating architectural patterns and best practices. For production deployment, refer to the production readiness analysis for additional considerations.
