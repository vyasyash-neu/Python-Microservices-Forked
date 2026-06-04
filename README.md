# 🏗️ Polyglot Microservices — E-Commerce Platform with AI

A production-grade microservices architecture built with **FastAPI**, **Spring Boot**, **Kafka**, **PostgreSQL**, **MongoDB**, **Elasticsearch**, **Redis**, and **Groq/Llama 3.3** — designed to demonstrate real-world patterns including event-driven communication, inter-service REST calls, JWT authentication, rate limiting, resilience patterns, full-text search, and AI integration.

**Polyglot architecture:** Python services for core e-commerce + AI, Java service for search — demonstrating that microservices allow each service to use the best language for the job.

---

## 📐 System Architecture

```
                        ┌─────────────────┐
                        │   Angular/React  │
                        │    Frontend      │
                        └────────┬────────┘
                                 │ HTTPS
                        ┌────────▼────────┐
                        │   API Gateway   │  ← Rate limiting, JWT validation
                        │   (FastAPI)     │
                        │   Port: 9000    │
                        └──┬──┬──┬──┬──┬─┘
                           │  │  │  │  │
       ┌───────────────────┘  │  │  │  └──────────────────┐
       │             ┌────────┘  │  └────────┐             │
       ▼             ▼           ▼           ▼             ▼
┌────────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌───────────┐
│  Product   │ │  Order   │ │Inventory │ │   AI   │ │  Search   │
│  Service   │ │  Service │ │ Service  │ │Service │ │  Service  │
│ Python     │ │ Python   │ │ Python   │ │Python  │ │  Java     │
│ Port: 8001 │ │Port: 8002│ │Port: 8003│ │Pt: 8005│ │ Port: 8006│
└─────┬──────┘ └────┬─────┘ └────┬─────┘ └───┬────┘ └─────┬─────┘
      │              │            │            │            │
   MongoDB      PostgreSQL   PostgreSQL   Groq/Llama  Elasticsearch
                     │
                ┌────▼──────────────────────┐
                │          KAFKA            │
                │  order-placed             │
                │  order-cancelled          │
                │  product-updated          │
                └────┬──────────┬───────────┘
                     │          │
          ┌──────────┤          ├───────────┐
          ▼          ▼          ▼           ▼
  ┌─────────────┐ ┌────────┐ ┌───────────┐
  │Notification │ │   AI   │ │  Search   │
  │  Service    │ │Service │ │  Service  │
  │ Port: 8004  │ │(Kafka  │ │(reindexes │
  │             │ │Consumer│ │on product │
  │  Redis      │ │+ LLM)  │ │ changes)  │
  └─────────────┘ └───┬────┘ └───────────┘
          ▲            │
          └────────────┘
          AI sends personalized
          content to Notification

  ┌─────────────────────────────────────────────────┐
  │              Keycloak (Port: 8081)              │
  │         OAuth2 / JWT Identity Provider          │
  └─────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────┐
  │           Observability Stack                   │
  │  Prometheus → Grafana │ Loki (logs) │ Tempo     │
  └─────────────────────────────────────────────────┘
```

---

## 🧩 Services Overview

| Service | Responsibility | Database | Port | Language | Status |
|---|---|---|---|---|---|
| **API Gateway** | Routing, JWT auth, rate limiting | None | 9000 | Python | ✅ Complete |
| **Product Service** | CRUD for product catalog | MongoDB | 8001 | Python | ✅ Complete |
| **Order Service** | Place & manage orders, Kafka producer | PostgreSQL + Outbox | 8002 | Python | ✅ Complete |
| **Inventory Service** | Stock management, stock verification | PostgreSQL | 8003 | Python | ✅ Complete |
| **Notification Service** | Email notifications via Kafka events | Redis (idempotency) | 8004 | Python | ✅ Complete |
| **AI Service** | Recommendations, chatbot, smart search | None (stateless) | 8005 | Python | ✅ Complete |
| **Search Service** | Full-text search, autocomplete, filters | Elasticsearch | 8006 | Java | ✅ Complete |

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Python Framework** | FastAPI (async-native) |
| **Java Framework** | Spring Boot 3.3 + Java 21 |
| **Language** | Python 3.12, Java 21 |
| **Databases** | PostgreSQL 16, MongoDB 7.0 |
| **Search Engine** | Elasticsearch 8.13 |
| **Cache / Idempotency** | Redis 7.2 |
| **Message Broker** | Apache Kafka (Confluent 7.6.0) |
| **ORM** | SQLAlchemy (async) for PostgreSQL, Motor (async) for MongoDB, Spring Data JPA, Spring Data Elasticsearch |
| **Validation** | Pydantic v2, Jakarta Bean Validation |
| **HTTP Client** | httpx (async), WebClient (Spring WebFlux) |
| **AI/LLM** | Groq (Llama 3.3 70B) — provider-agnostic, supports Gemini & Ollama |
| **Authentication** | Keycloak 24.0 (OAuth2 / JWT) + PyJWT |
| **Rate Limiting** | slowapi |
| **Resilience** | tenacity (retry), custom async circuit breaker |
| **Observability** | Prometheus, Grafana, Loki, Tempo |
| **Containerization** | Docker, Docker Compose |
| **Testing** | pytest, pytest-asyncio, unittest.mock, JUnit 5, Mockito |

---

## 📦 Project Structure

```
Python-Microservices/
│
├── api-gateway/                          # Python — FastAPI
│   ├── app/
│   │   ├── main.py                       # Proxy routes, shared httpx client
│   │   ├── config.py                     # Service URLs, Keycloak, rate limits
│   │   ├── auth/
│   │   │   └── keycloak.py               # JWT validation via JWKS
│   │   └── middleware/
│   │       └── rate_limit.py             # slowapi rate limiter
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
│
├── product-service/                      # Python — FastAPI + MongoDB
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── database.py                   # Motor async MongoDB client
│   │   ├── schemas/
│   │   │   └── product.py
│   │   ├── routes/
│   │   │   └── product_routes.py
│   │   └── services/
│   │       └── product_service.py
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
│
├── order-service/                        # Python — FastAPI + PostgreSQL + Kafka
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── database.py                   # SQLAlchemy async + PostgreSQL
│   │   ├── models/
│   │   │   ├── order.py                  # Orders + order_items ORM
│   │   │   └── outbox.py                 # Outbox table for guaranteed delivery
│   │   ├── schemas/
│   │   │   └── order.py
│   │   ├── routes/
│   │   │   └── order_routes.py
│   │   ├── services/
│   │   │   ├── order_service.py
│   │   │   └── outbox_worker.py          # Background worker: outbox → Kafka
│   │   ├── clients/
│   │   │   └── inventory_client.py
│   │   └── kafka/
│   │       └── producer.py
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
│
├── inventory-service/                    # Python — FastAPI + PostgreSQL
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── database.py
│   │   ├── models/
│   │   │   └── inventory.py
│   │   ├── schemas/
│   │   │   └── inventory.py
│   │   ├── routes/
│   │   │   └── inventory_routes.py
│   │   └── services/
│   │       └── inventory_service.py
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
│
├── notification-service/                 # Python — FastAPI + Kafka + Redis
│   ├── app/
│   │   ├── main.py                       # FastAPI + Kafka consumer background task
│   │   ├── config.py
│   │   ├── kafka/
│   │   │   └── consumer.py               # aiokafka consumer for 4 topics
│   │   └── services/
│   │       └── email_service.py          # Gmail SMTP + HTML email templates
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
│
├── ai-service/                           # Python — FastAPI + Kafka + LLM + Redis cache
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── llm/
│   │   │   ├── base.py                   # Abstract LLMClient interface
│   │   │   ├── gemini_client.py          # Google Gemini
│   │   │   ├── groq_client.py            # Groq / Llama 3.3 70B
│   │   │   ├── ollama_client.py          # Ollama (local)
│   │   │   └── factory.py                # Provider factory
│   │   ├── clients/
│   │   │   └── product_client.py         # Fetches real catalog for LLM context
│   │   ├── cache/
│   │   │   └── redis_cache.py            # Cache-aside Redis layer (DB 1)
│   │   ├── routes/
│   │   │   └── ai_routes.py
│   │   ├── services/
│   │   │   ├── chatbot.py
│   │   │   ├── recommendation.py
│   │   │   ├── suggestion.py
│   │   │   └── notification_ai.py
│   │   └── kafka/
│   │       ├── consumer.py
│   │       └── producer.py
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
│
├── search-service/                       # Java — Spring Boot + Elasticsearch
│   ├── src/main/java/
│   │   └── com/ecommerce/search/
│   │       ├── SearchApplication.java
│   │       ├── config/
│   │       ├── controller/
│   │       ├── model/
│   │       ├── repository/
│   │       ├── service/
│   │       └── kafka/
│   ├── src/test/java/
│   │   └── com/ecommerce/search/service/
│   │       └── SearchServiceTest.java    # JUnit 5 + Mockito unit tests
│   ├── Dockerfile
│   └── pom.xml
│
├── docker/
│   ├── postgres/
│   │   └── init-multiple-dbs.sh
│   ├── keycloak/
│   │   └── realm-export.json
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── grafana/
│   │   └── provisioning/
│   ├── loki/
│   │   └── loki-config.yaml
│   ├── promtail/
│   │   └── promtail-config.yaml
│   └── tempo/
│       └── tempo-config.yaml
│
├── docker-compose.yml
└── README.md
```

---

## 🔗 Inter-Service Communication

### Synchronous (REST/HTTP)
```
API Gateway    → All Services (proxy)
Order Service  → Inventory Service (stock check + reduce)
AI Service     → Product Service (fetch catalog for LLM context)
```

### Asynchronous (Kafka)
```
Order Service ──► [order-placed]          ──► Notification Service (confirmation email)
Order Service ──► [order-placed]          ──► AI Service (personalize email)
AI Service    ──► [ai-notification-ready] ──► Notification Service (personalized email)
Order Service ──► [order-cancelled]       ──► Notification Service (cancellation email)
Inventory Svc ──► [inventory-low]         ──► Notification Service (low stock alert)
Product Svc   ──► [product-updated]       ──► Search Service (reindex in Elasticsearch)
```

### Asynchronous (Kafka + Outbox Pattern)
```
Order Service:
  BEGIN TRANSACTION
    1. Save order to PostgreSQL
    2. Save event to outbox table (same transaction)
  COMMIT

  Background worker:
    3. Read PENDING events from outbox
    4. Publish to Kafka
    5. Mark as SENT
    6. Cleanup after 7 days

  → Guaranteed delivery — events survive Kafka outages
```

### Kafka Topics

| Topic | Producer | Consumers | Purpose |
|---|---|---|---|
| `order-placed` | Order Service | Notification, AI Service | New order created |
| `order-cancelled` | Order Service | Notification Service | Order cancelled |
| `inventory-low` | Inventory Service | Notification Service | Stock alert |
| `ai-notification-ready` | AI Service | Notification Service | Personalized email ready |
| `product-updated` | Product Service | Search Service | Reindex product in Elasticsearch |

---

## 🗄️ Database Schemas

### Product Service — MongoDB
```json
Collection: products
{
  "_id": "ObjectId",
  "name": "iPhone 15 Pro",
  "description": "Latest Apple smartphone",
  "price": 999.99,
  "category": "Electronics",
  "tags": ["smartphone", "apple", "5g"],
  "stock_quantity": 100,
  "image_url": "https://...",
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

### Order Service — PostgreSQL
```
Table: orders
  id (UUID, PK) | order_number | customer_name | customer_email
  total_amount   | status (ENUM) | created_at   | updated_at

  Status: PENDING → CONFIRMED → SHIPPED → DELIVERED → CANCELLED

Table: order_items
  id (UUID, PK) | order_id (FK) | product_id | product_name
  quantity       | unit_price    | total_price

Table: outbox
  id (UUID, PK) | topic | event_payload (JSON) | status (PENDING/SENT)
  created_at     | sent_at
```

### Inventory Service — PostgreSQL
```
Table: inventory
  id (UUID, PK)  | product_id (unique) | product_name
  quantity        | reserved_qty        | created_at | updated_at

  Computed: available_qty = quantity - reserved_qty
```

### Search Service — Elasticsearch
```json
Index: products
{
  "name": "iPhone 15 Pro",
  "description": "Latest Apple smartphone",
  "price": 999.99,
  "category": "Electronics",
  "tags": ["smartphone", "apple", "5g"],
  "suggest": {
    "input": ["iPhone", "iPhone 15", "iPhone 15 Pro"]
  }
}
```

---

## 🔌 API Endpoints

> **All requests go through the API Gateway on port 9000.**

### API Gateway (Port 9000)
| Method | Path | Proxies To | Rate Limit |
|---|---|---|---|
| `*` | `/api/products/**` | Product Service :8001 | 60/min |
| `*` | `/api/orders/**` | Order Service :8002 | 60/min |
| `*` | `/api/inventory/**` | Inventory Service :8003 | 60/min |
| `*` | `/api/ai/**` | AI Service :8005 | 15/min |
| `*` | `/api/search/**` | Search Service :8006 | 60/min |

### Product Service
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/products` | Create a new product |
| `GET` | `/api/products` | List all products |
| `GET` | `/api/products/search?q=` | Search by name, category, or tags |
| `GET` | `/api/products/{id}` | Get product by ID |
| `PUT` | `/api/products/{id}` | Update product |
| `DELETE` | `/api/products/{id}` | Delete product |

### Order Service
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders` | Get all orders |
| `GET` | `/api/orders/{order_id}` | Get order by ID |
| `GET` | `/api/orders/user/{email}` | Get orders by customer email |
| `PATCH` | `/api/orders/{order_id}/status` | Update order status |
| `PATCH` | `/api/orders/{order_id}/cancel` | Cancel an order |

### Inventory Service
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/inventory` | Add inventory item |
| `GET` | `/api/inventory` | List all inventory |
| `GET` | `/api/inventory/{product_id}` | Get stock for product |
| `GET` | `/api/inventory/{product_id}/check?quantity=N` | Check stock availability |
| `PATCH` | `/api/inventory/{product_id}/reduce` | Reduce stock |
| `PATCH` | `/api/inventory/{product_id}/restock` | Restock item |

### AI Service
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ai/chat` | Shopping assistant chatbot |
| `GET` | `/api/ai/recommendations` | Product recommendations |
| `POST` | `/api/ai/suggest` | Natural language product search |

### Search Service (Java)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/search?q=` | Full-text product search (fuzzy via `multi_match` + `AUTO` fuzziness) |
| `GET` | `/api/search/autocomplete?q=` | Autocomplete suggestions |
| `GET` | `/api/search/filter?category=&minPrice=&maxPrice=` | Faceted filtering |

### Notification Service
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Health check (no REST API — event-driven only) |

---

## 🔄 End-to-End Order Flow

```
1.  Client → POST :9000/api/orders (via API Gateway)
2.  Gateway validates JWT → proxies to Order Service
3.  Order Service → GET /api/inventory/{id}/check (verify stock)
4.  If in stock → save order to PostgreSQL
5.  Save event to outbox table (same transaction — guaranteed)
6.  Order Service → PATCH /api/inventory/{id}/reduce (reduce stock)
7.  Background worker reads outbox → publishes 'order-placed' to Kafka
8.  Worker marks event as SENT in outbox
9.  Notification Service → consumes event → checks Redis (idempotency)
10. If new → logs/sends confirmation email
11. AI Service → consumes event → generates personalized email via LLM
12. AI Service → publishes 'ai-notification-ready' to Kafka
13. Notification Service → consumes AI event → checks Redis → logs/sends personalized email
14. Return 201 Created to client
```

---

## 🤖 AI Features

| Feature | Endpoint | LLM Provider | Description |
|---|---|---|---|
| Shopping Chatbot | `POST /api/ai/chat` | Groq (Llama 3.3 70B) | Conversational assistant with real product context |
| Recommendations | `GET /api/ai/recommendations` | Groq (Llama 3.3 70B) | 5 related products from real catalog |
| Smart Search | `POST /api/ai/suggest` | Groq (Llama 3.3 70B) | Natural language → matching products |
| Email Personalization | Kafka event | Groq (Llama 3.3 70B) | AI-generated follow-up emails |

### Provider-Agnostic Design
The AI Service supports 3 LLM providers. Switch by changing one line in `.env`:
```
LLM_PROVIDER=groq      # Groq / Llama 3.3 70B (current)
LLM_PROVIDER=gemini    # Google Gemini
LLM_PROVIDER=ollama    # Ollama (local, no API key)
```

### Redis Caching (Cache-Aside)
The AI Service caches LLM-returned product IDs (6h TTL) and catalog data (15min TTL) in Redis DB 1 to reduce LLM calls and Product Service load. Live product details (prices, inventory) are always fetched fresh. Chat responses are not cached since conversation history makes full-response caching unsafe.

---

## 🔍 Search Features (Java Spring Boot)

| Feature | Endpoint | Description |
|---|---|---|
| Full-text search | `GET /api/search?q=` | `multi_match` across name/description/tags/category with `AUTO` fuzziness and `name^3` boost |
| Autocomplete | `GET /api/search/autocomplete?q=` | Prefix-based type-ahead suggestions |
| Faceted filters | `GET /api/search/filter?category=&minPrice=&maxPrice=` | Filter by category, price range |

### How Search Stays in Sync
```
Product Service (Python) → Kafka: product-updated → Search Service (Java) → Elasticsearch reindex
```

Products are the source of truth in MongoDB. Elasticsearch is a read-optimized copy that stays in sync via Kafka events. If Elasticsearch goes down, the Product Service still works — search is just temporarily unavailable.

---

## 🔐 Security

```
1. User logs in via Keycloak → gets JWT access token
2. Client sends JWT: Authorization: Bearer <token>
3. API Gateway validates JWT with Keycloak public keys (RS256)
4. If valid → strips auth header, forwards to downstream service
5. If invalid → 401 Unauthorized
6. If expired → 403 Forbidden
7. Services trust all requests from Gateway (internal network)
```

> **Note:** `AUTH_ENABLED=false` by default for development. Set to `true` when Keycloak is configured.

---

## 🛡️ Resilience Patterns

| Pattern | Library | Applied At | Fallback |
|---|---|---|---|
| **Outbox Pattern** | PostgreSQL | Order Service → Kafka | Events survive Kafka outages |
| **Idempotency** | Redis `SET NX` | Notification Service | Prevents duplicate emails |
| **Circuit Breaker** | Custom async (80 LOC) | Order → Inventory | Return "service unavailable" |
| **Retry + Backoff** | tenacity | Order → Inventory, AI → LLM | Raise after max retries |
| **Timeout** | httpx | All inter-service calls | Raise timeout exception |
| **Rate Limiter** | slowapi | API Gateway | 429 Too Many Requests |
| **Cache-Aside** | Redis | AI Service | Graceful fallback on Redis failure |

---

## 🧪 Testing

| Service | Language | Test Files | Tests | What's Covered |
|---|---|---|---|---|
| API Gateway | Python | `test_gateway.py`, `test_auth.py` | 30 | Routing, proxying, error handling, JWT |
| Product Service | Python | `test_product_service.py` | — | CRUD, search, validation |
| Order Service | Python | `test_order_service.py` | 15 | Order creation, stock checks, cancellation, Kafka |
| Inventory Service | Python | `test_inventory_service.py` | 20 | CRUD, stock check, reduce, restock |
| Notification Service | Python | `test_email_service.py`, `test_kafka_consumer.py` | 34 | Email templates, SMTP, Kafka routing |
| AI Service | Python | `test_llm_clients.py`, `test_ai_services.py`, `test_product_client.py`, `test_kafka.py` | 44 | All 3 LLM providers, all 4 AI features |
| Search Service | Java | `SearchServiceTest.java` | 15 | Full-text search, fuzzy match, autocomplete, faceted filter, indexing |

**Total: 158+ unit tests across all services**

### Running Tests
```bash
# Python services
cd <service-directory>
source venv/bin/activate
pytest -v

# Java service
cd search-service
mvn test
```

---

## 🐳 Infrastructure (Docker Compose)

| Service | Port | Purpose |
|---|---|---|
| MongoDB | 27017 | Product Service database |
| PostgreSQL | 5433 | Order + Inventory databases |
| Elasticsearch | 9200 | Search Service — full-text search |
| Redis | 6379 | Notification Service (DB 0) + AI Service cache (DB 1) |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |
| Kafka UI | 8090 | Visual Kafka management |
| Keycloak | 8081 | OAuth2 / JWT identity provider |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Dashboards |
| Loki | 3100 | Log aggregation |
| Tempo | 3200 | Distributed tracing |

### Starting Infrastructure
```bash
cd Python-Microservices
docker-compose up -d
```

### Verifying Services
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

---

## 🚀 Running the Application

### Prerequisites
- macOS with Homebrew
- Python 3.12 (via Homebrew, **not** Anaconda)
- Java 21 (for Search Service)
- Docker Desktop
- Groq API key (free at https://console.groq.com/keys)

### Step 1 — Start Infrastructure
```bash
cd Python-Microservices
docker-compose up -d
```

### Step 2 — Start Services (each in a separate terminal)

```bash
# Terminal 1: Product Service (Python)
cd product-service && source venv/bin/activate
python -m uvicorn app.main:app --port 8001 --loop asyncio

# Terminal 2: Inventory Service (Python)
cd inventory-service && source venv/bin/activate
python -m uvicorn app.main:app --port 8003 --loop asyncio

# Terminal 3: Order Service (Python)
cd order-service && source venv/bin/activate
python -m uvicorn app.main:app --port 8002 --loop asyncio

# Terminal 4: Notification Service (Python)
cd notification-service && source venv/bin/activate
python -m uvicorn app.main:app --port 8004 --loop asyncio

# Terminal 5: AI Service (Python)
cd ai-service && source venv/bin/activate
python -m uvicorn app.main:app --port 8005 --loop asyncio

# Terminal 6: Search Service (Java)
cd search-service
mvn spring-boot:run

# Terminal 7: API Gateway (Python)
cd api-gateway && source venv/bin/activate
python -m uvicorn app.main:app --port 9000 --loop asyncio
```

### Step 3 — Test via Gateway
```bash
# Health check
curl http://localhost:9000/health

# Add inventory
curl -X POST http://localhost:9000/api/inventory \
  -H "Content-Type: application/json" \
  -d '{"product_id": "prod-001", "product_name": "iPhone 15 Pro", "quantity": 100}'

# Place order (triggers full Kafka flow)
curl -X POST http://localhost:9000/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer_name": "Yash Vyas",
    "customer_email": "yash@example.com",
    "items": [{
      "product_id": "prod-001",
      "product_name": "iPhone 15 Pro",
      "quantity": 1,
      "unit_price": 999.99
    }]
  }'

# AI chatbot
curl -X POST http://localhost:9000/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What products do you have?"}'

# Full-text search (Java service) — fuzzy: "iphne" still matches iPhone
curl "http://localhost:9000/api/search?q=iphone"
curl "http://localhost:9000/api/search?q=iphne"

# Autocomplete
curl "http://localhost:9000/api/search/autocomplete?q=iph"

# Faceted filter
curl "http://localhost:9000/api/search/filter?category=Electronics&minPrice=500&maxPrice=1500"
```

---

## 📝 Service Documentation

| Service | Language | Documentation |
|---|---|---|
| Product Service | Python | [`product-service/product-service-docs.md`](product-service/product-service-docs.md) |
| Order Service | Python | [`order-service/order-docs.md`](order-service/order-docs.md) |
| Inventory Service | Python | [`inventory-service/inventory-docs.md`](inventory-service/inventory-docs.md) |
| Notification Service | Python | [`notification-service/notification-docs.md`](notification-service/notification-docs.md) |
| AI Service | Python | [`ai-service/ai-service-docs.md`](ai-service/ai-service-docs.md) |
| API Gateway | Python | [`api-gateway/api-gateway-docs.md`](api-gateway/api-gateway-docs.md) |
| Search Service | Java | [`search-service/search-service-docs.md`](search-service/search-service-docs.md) |

---

## 🐛 Notable Issues & Fixes

| Issue | Root Cause | Fix |
|---|---|---|
| Anaconda interfering with async event loop | venv inherited Anaconda's sys.path | Removed Anaconda, used Homebrew Python |
| MongoDB auth failing from host to Docker | SCRAM auth broken over Docker TCP bridge on Mac | Disabled auth for local dev |
| `motor` + `pymongo` version incompatibility | Motor relied on removed PyMongo internals | Pinned compatible versions |
| PostgreSQL init script not running | Data volume already initialized | `docker-compose down -v` to reset |
| Port conflicts on Mac (8080, 5432) | Local processes occupying ports | Remapped to 8081, 5433 |
| SQLAlchemy async missing `greenlet` | Not auto-installed as dependency | Added to requirements.txt |
| Missing `__init__.py` files | Python can't find packages | Created in all directories |
| Gemini daily rate limit exhausted | Kafka burst + retry cascading | Switched to Groq, added throttling |
| Product Service response format mismatch | Returns dict not list | Handle both formats in client |
| Pydantic rejecting extra `.env` fields | Fields not declared in Settings | Added all fields to config.py |
| `pybreaker` incompatible with Python 3.12 | Tornado dependency broke | Replaced with custom 80-line async circuit breaker |
| Lombok failing on Java 21 + Maven | Annotation processing issues | Removed Lombok, used explicit getters/setters/builders |
| Local Homebrew Redis conflicting with Docker Redis | Both bound to 6379 | `brew services stop redis` before `docker-compose up` |
| Spring Data ES `Criteria.matches()` not fuzzy | API doesn't expose fuzziness directly | Switched to `StringQuery` with raw `multi_match` + `AUTO` fuzziness |
| Mockito failing on Java 25 Byte Buddy | Byte Buddy only supports up to Java 23 | Pinned project to Java 21 in `pom.xml` |

---

## 🗺️ Build Roadmap

```
Phase 1 ✅ Infrastructure
  └── Docker Compose (Kafka, PostgreSQL, MongoDB, Keycloak, Observability)

Phase 2 ✅ Core Services
  ├── Product Service (Python, MongoDB, Motor async)
  ├── Inventory Service (Python, PostgreSQL, SQLAlchemy async)
  └── Order Service (Python, PostgreSQL, httpx, aiokafka)

Phase 3 ✅ Async Layer
  └── Notification Service (Python, Kafka consumer, Gmail SMTP)

Phase 4 ✅ AI Layer
  └── AI Service (Python, Groq/Llama 3.3 70B, provider-agnostic, Kafka, Redis caching)

Phase 5 ✅ Gateway & Security
  └── API Gateway (Python, routing, JWT via Keycloak, rate limiting)

Phase 6 ✅ Resilience & Reliability
  ├── Outbox Pattern in Order Service (PostgreSQL — guaranteed Kafka delivery)
  ├── Idempotency in Notification Service (Redis SET NX)
  ├── Custom async Circuit Breaker on Order → Inventory
  ├── Retry + Backoff on Order → Inventory, AI → LLM (tenacity)
  └── Timeouts on all HTTP calls (httpx)

Phase 7 ✅ Search Service (Java Spring Boot)
  ├── Spring Boot 3.3 + Java 21
  ├── Elasticsearch full-text search (multi_match + AUTO fuzziness + name^3 boost)
  ├── Autocomplete + faceted filtering
  ├── Kafka consumer (reindex on product-updated)
  └── JUnit 5 + Mockito tests (15 tests)

Phase 8 🔲 Observability
  └── Prometheus, Grafana, Loki, Tempo wiring

Phase 9 🔲 CI/CD
  └── GitHub Actions (lint → test → build → deploy)
```

---

## 📄 License

This project is built for learning and portfolio purposes.

---

## 👤 Author

**Yash Vyas**
