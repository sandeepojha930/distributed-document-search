# API Examples

## Sample API Requests

### 1. Create a Document

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -d '{
    "title": "Introduction to Distributed Systems",
    "content": "Distributed systems are collections of independent computers that appear to users as a single coherent system. They enable scalability, fault tolerance, and geographic distribution.",
    "metadata": {
      "author": "John Doe",
      "category": "technical",
      "tags": ["distributed-systems", "architecture"]
    }
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "tenant-123",
  "title": "Introduction to Distributed Systems",
  "content": "Distributed systems are collections of independent computers...",
  "status": "INDEXING",
  "metadata": {
    "author": "John Doe",
    "category": "technical",
    "tags": ["distributed-systems", "architecture"]
  },
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### 2. Search Documents

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/search?q=distributed%20systems&tenant=tenant-123&page=1&size=10" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "query": "distributed systems",
  "total": 25,
  "page": 1,
  "size": 10,
  "results": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Introduction to Distributed Systems",
      "snippet": "Distributed systems are collections of independent computers that appear to users as a single coherent system...",
      "score": 0.95,
      "metadata": {
        "author": "John Doe",
        "category": "technical"
      }
    }
  ]
}
```

### 3. Get Document by ID

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/documents/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-Tenant-Id: tenant-123" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "tenant-123",
  "title": "Introduction to Distributed Systems",
  "content": "Distributed systems are collections of independent computers that appear to users as a single coherent system. They enable scalability, fault tolerance, and geographic distribution.",
  "status": "INDEXED",
  "metadata": {
    "author": "John Doe",
    "category": "technical",
    "tags": ["distributed-systems", "architecture"]
  },
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

### 4. Delete Document

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/v1/documents/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-Tenant-Id: tenant-123"
```

**Response:**
```
204 No Content
```

### 5. Health Check

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/health
```

**Response:**
```json
{
  "status": "UP",
  "checks": {
    "postgresql": "UP",
    "elasticsearch": "UP",
    "redis": "UP",
    "rabbitmq": "UP"
  }
}
```

## Postman Collection

You can import the following JSON into Postman:

```json
{
  "info": {
    "name": "Document Search Service",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Document",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "X-Tenant-Id",
            "value": "tenant-123"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"title\": \"Sample Document\",\n  \"content\": \"This is sample content for indexing.\",\n  \"metadata\": {\n    \"author\": \"John Doe\",\n    \"category\": \"sample\"\n  }\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/v1/documents",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "documents"]
        }
      }
    },
    {
      "name": "Search Documents",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/search?q=distributed&tenant=tenant-123&page=1&size=10",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "search"],
          "query": [
            {"key": "q", "value": "distributed"},
            {"key": "tenant", "value": "tenant-123"},
            {"key": "page", "value": "1"},
            {"key": "size", "value": "10"}
          ]
        }
      }
    },
    {
      "name": "Get Document",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "X-Tenant-Id",
            "value": "tenant-123"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/v1/documents/:id",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "documents", ":id"],
          "variable": [
            {
              "key": "id",
              "value": "550e8400-e29b-41d4-a716-446655440000"
            }
          ]
        }
      }
    },
    {
      "name": "Delete Document",
      "request": {
        "method": "DELETE",
        "header": [
          {
            "key": "X-Tenant-Id",
            "value": "tenant-123"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/v1/documents/:id",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "documents", ":id"],
          "variable": [
            {
              "key": "id",
              "value": "550e8400-e29b-41d4-a716-446655440000"
            }
          ]
        }
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/health",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "health"]
        }
      }
    }
  ]
}
```

## Testing with Multiple Tenants

### Tenant 1 (tenant-123)
```bash
# Create document for tenant-123
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -d '{"title": "Tenant 1 Document", "content": "This is a document for tenant 1"}'

# Search only returns tenant-123 documents
curl "http://localhost:8080/api/v1/search?q=document&tenant=tenant-123"
```

### Tenant 2 (tenant-456)
```bash
# Create document for tenant-456
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-456" \
  -d '{"title": "Tenant 2 Document", "content": "This is a document for tenant 2"}'

# Search only returns tenant-456 documents
curl "http://localhost:8080/api/v1/search?q=document&tenant=tenant-456"
```

## Rate Limiting Test

Test rate limiting by making rapid requests:

```bash
# Make 100+ requests rapidly
for i in {1..110}; do
  curl -X GET "http://localhost:8080/api/v1/search?q=test&tenant=tenant-123" \
    -H "X-Tenant-Id: tenant-123"
  echo "Request $i"
done

# After 100 requests, you should receive 429 Too Many Requests
```

## Error Scenarios

### Missing Tenant ID
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "content": "Test content"}'

# Response: 400 Bad Request
```

### Document Not Found
```bash
curl -X GET http://localhost:8080/api/v1/documents/00000000-0000-0000-0000-000000000000 \
  -H "X-Tenant-Id: tenant-123"

# Response: 404 Not Found
```

### Invalid Request Body
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -d '{"title": ""}'

# Response: 400 Bad Request (validation error)
```
