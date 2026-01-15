# Distributed Document Search Service - Architecture Design

## 1. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Load Balancer                            │
│                      (Nginx/HAProxy/ALB)                         │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
┌───────▼────────┐                    ┌────────▼───────┐
│  API Gateway   │                    │  API Gateway   │
│  (Instance 1)  │                    │  (Instance N)  │
└───────┬────────┘                    └────────┬───────┘
        │                                       │
        └───────────────┬───────────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
┌───────▼────────┐              ┌───────▼────────┐
│   Redis        │              │   Redis        │
│   (Cache)      │              │   (Cache)      │
└───────┬────────┘              └───────┬────────┘
        │                               │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
┌───────▼────────┐              ┌───────▼────────┐
│ Elasticsearch  │              │ Elasticsearch  │
│   Cluster      │              │   Cluster      │
│  (Primary)     │              │  (Replica)     │
└───────┬────────┘              └───────┬────────┘
        │                               │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
┌───────▼────────┐              ┌───────▼────────┐
│  PostgreSQL    │              │  RabbitMQ      │
│  (Metadata)    │              │  (Message Q)   │
└────────────────┘              └────────────────┘
```

## 2. Data Flow Diagrams

### 2.1 Document Indexing Flow

```
Client Request
    │
    ▼
API Gateway (Rate Limit Check)
    │
    ▼
Document Service
    │
    ├──► Validate & Sanitize
    │
    ├──► Store Metadata → PostgreSQL
    │
    ├──► Cache Document → Redis (TTL: 1h)
    │
    └──► Publish Index Task → RabbitMQ
            │
            ▼
        Index Worker (Async)
            │
            ├──► Extract Content
            │
            ├──► Build Search Index → Elasticsearch
            │
            └──► Update Status → PostgreSQL
```

### 2.2 Search Flow

```
Client Request (GET /search?q=query&tenant=tenantId)
    │
    ▼
API Gateway
    │
    ├──► Rate Limit Check (Redis)
    │
    ├──► Check Cache (Redis) ──► Cache Hit ──► Return Cached Result
    │
    └──► Cache Miss
            │
            ▼
        Search Service
            │
            ├──► Build Elasticsearch Query (with tenant filter)
            │
            ├──► Execute Search → Elasticsearch
            │
            ├──► Retrieve Metadata → PostgreSQL (batch)
            │
            ├──► Rank & Format Results
            │
            ├──► Cache Results → Redis (TTL: 5min)
            │
            └──► Return Response
```

## 3. Database/Storage Strategy

### 3.1 Elasticsearch (Search Engine)
**Choice Rationale:**
- Optimized for full-text search with sub-second query performance
- Built-in relevance scoring (BM25 algorithm)
- Horizontal scalability with sharding
- Supports advanced features (fuzzy search, faceted search, highlighting)
- Native multi-tenancy support via index aliases or tenant field

**Index Strategy:**
- One index per tenant OR single index with tenant field (chosen: single index with tenant field for simplicity)
- Sharding: 3 primary shards per index (configurable)
- Replication: 1 replica per shard for fault tolerance
- Index naming: `documents_v1` (versioned for zero-downtime updates)

### 3.2 PostgreSQL (Metadata Store)
**Choice Rationale:**
- ACID compliance for document metadata
- Relational queries for document management
- JSONB support for flexible document schemas
- Strong consistency for critical operations

**Schema:**
- `documents` table: id, tenant_id, title, content_hash, status, created_at, updated_at
- `document_metadata` table: document_id, key, value (JSONB)
- Indexes: tenant_id, status, created_at

### 3.3 Redis (Cache Layer)
**Choice Rationale:**
- Sub-millisecond latency for cache operations
- TTL support for automatic expiration
- Distributed caching across instances
- Used for both search results and rate limiting

**Cache Strategy:**
- Search results: Key pattern `search:{tenant}:{query_hash}`, TTL: 5 minutes
- Document cache: Key pattern `doc:{tenant}:{doc_id}`, TTL: 1 hour
- Rate limiting: Key pattern `ratelimit:{tenant}:{window}`, TTL: window duration

## 4. API Design

### 4.1 Endpoints

#### POST /api/v1/documents
**Purpose:** Index a new document

**Request:**
```json
{
  "title": "Document Title",
  "content": "Document content for indexing...",
  "metadata": {
    "author": "John Doe",
    "category": "technical"
  }
}
```

**Headers:**
- `X-Tenant-Id: tenant-123` (required)

**Response:** `201 Created`
```json
{
  "id": "doc-uuid-123",
  "tenantId": "tenant-123",
  "title": "Document Title",
  "status": "indexing",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### GET /api/v1/search?q={query}&tenant={tenantId}
**Purpose:** Search documents

**Query Parameters:**
- `q` (required): Search query
- `tenant` (required): Tenant ID
- `page` (optional): Page number (default: 1)
- `size` (optional): Results per page (default: 10)
- `sort` (optional): Sort order (relevance, date)

**Response:** `200 OK`
```json
{
  "query": "search term",
  "total": 150,
  "page": 1,
  "size": 10,
  "results": [
    {
      "id": "doc-uuid-123",
      "title": "Document Title",
      "snippet": "This is a <em>search term</em> in context...",
      "score": 0.95,
      "metadata": {
        "author": "John Doe"
      }
    }
  ]
}
```

#### GET /api/v1/documents/{id}
**Purpose:** Retrieve document details

**Headers:**
- `X-Tenant-Id: tenant-123` (required)

**Response:** `200 OK`
```json
{
  "id": "doc-uuid-123",
  "tenantId": "tenant-123",
  "title": "Document Title",
  "content": "Full document content...",
  "metadata": {
    "author": "John Doe",
    "category": "technical"
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### DELETE /api/v1/documents/{id}
**Purpose:** Remove a document

**Headers:**
- `X-Tenant-Id: tenant-123` (required)

**Response:** `204 No Content`

#### GET /api/v1/health
**Purpose:** Health check with dependency status

**Response:** `200 OK`
```json
{
  "status": "UP",
  "checks": {
    "elasticsearch": "UP",
    "postgresql": "UP",
    "redis": "UP",
    "rabbitmq": "UP"
  }
}
```

## 5. Consistency Model and Trade-offs

### 5.1 Consistency Model
- **Eventual Consistency** for search index (Elasticsearch)
- **Strong Consistency** for document metadata (PostgreSQL)
- **Cache Consistency**: TTL-based invalidation

### 5.2 Trade-offs

**Indexing Latency vs. Consistency:**
- Documents indexed asynchronously via message queue
- Trade-off: Search may not immediately reflect new documents (acceptable for most use cases)
- Benefit: API responds quickly (<100ms), system handles load spikes

**Cache Freshness vs. Performance:**
- Search results cached for 5 minutes
- Trade-off: Stale results possible for 5 minutes
- Benefit: 10x reduction in Elasticsearch load, sub-50ms response times

**Multi-Tenancy: Single Index vs. Index per Tenant:**
- Chosen: Single index with tenant field
- Trade-off: Potential for cross-tenant data leakage if query bug exists
- Mitigation: Always filter by tenant_id in queries, application-level validation
- Benefit: Simpler operations, shared resources, easier cross-tenant analytics

## 6. Caching Strategy

### 6.1 Cache Layers

**L1: Application Cache (In-Memory)**
- Hot queries cached locally (Caffeine cache)
- Size: 1000 entries per instance
- TTL: 1 minute
- Purpose: Reduce Redis round-trips for popular queries

**L2: Distributed Cache (Redis)**
- Search results: 5-minute TTL
- Document cache: 1-hour TTL
- Rate limit counters: Window-based TTL
- Eviction: LRU when memory limit reached

**L3: Elasticsearch Query Cache**
- Elasticsearch internal query cache
- Automatically managed by Elasticsearch
- Benefits: Repeated queries served from cache

### 6.2 Cache Invalidation
- **Write-through**: On document update/delete, invalidate cache
- **TTL-based**: Automatic expiration for search results
- **Manual**: Admin endpoint for cache clearing (if needed)

## 7. Message Queue Usage

### 7.1 RabbitMQ Configuration
- **Exchange**: `document-exchange` (topic)
- **Queues**: 
  - `document.index` (durable, 10 workers)
  - `document.delete` (durable, 5 workers)
- **Routing Keys**: 
  - `document.index.{tenantId}`
  - `document.delete.{tenantId}`

### 7.2 Asynchronous Operations
1. **Document Indexing**: 
   - API returns immediately with "indexing" status
   - Worker processes indexing asynchronously
   - Status updated in PostgreSQL when complete

2. **Bulk Operations**:
   - Bulk imports queued for processing
   - Prevents API timeouts

3. **Index Updates**:
   - Document updates trigger re-indexing via queue
   - Ensures eventual consistency

## 8. Multi-Tenancy Approach

### 8.1 Data Isolation Strategy

**Application-Level Isolation:**
- Tenant ID extracted from HTTP header (`X-Tenant-Id`) or path parameter
- All queries filtered by tenant_id
- No cross-tenant data access possible at application layer

**Database-Level Isolation:**
- PostgreSQL: Row-level filtering via WHERE tenant_id = ?
- Elasticsearch: Filter clause `tenant_id: {tenantId}` in every query
- Redis: Tenant ID in cache key prefix

**Security:**
- Tenant ID validated against authorized tenant list (can be extended to JWT validation)
- API Gateway can enforce tenant validation before reaching application

### 8.2 Tenant Management
- Tenant configuration stored in PostgreSQL `tenants` table
- Features: rate limits, storage quotas, feature flags
- Admin API for tenant management (not implemented in prototype)

## 9. Scalability Considerations

### 9.1 Horizontal Scaling
- **API Instances**: Stateless, scale horizontally behind load balancer
- **Elasticsearch**: Add nodes to cluster, shards distributed automatically
- **Workers**: Scale message queue consumers independently
- **Redis**: Redis Cluster for distributed caching

### 9.2 Performance Targets
- **Search Latency**: <500ms (95th percentile)
- **Indexing Latency**: <100ms API response, async processing
- **Throughput**: 1000+ searches/second (with caching)
- **Capacity**: 10M+ documents (Elasticsearch handles billions)

## 10. Technology Stack Summary

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| API Framework | Spring Boot | Enterprise-grade, mature ecosystem |
| Search Engine | Elasticsearch | Industry standard for full-text search |
| Database | PostgreSQL | ACID compliance, JSONB support |
| Cache | Redis | Sub-millisecond latency |
| Message Queue | RabbitMQ | Reliable, feature-rich |
| Containerization | Docker Compose | Easy local development |
| Language | Java | Type safety, performance, enterprise adoption |
