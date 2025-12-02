# Event Manager Service

Microservice for managing events in the Planify application.

## 🎯 Responsibilities

- Create, read, update, delete (CRUD) events
- Manage event details (title, description, date, location)
- Publish events to Kafka for other services
- Provide event data to other microservices

## 🔌 API Endpoints

### Events

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | Get all events |
| GET | `/api/events/{id}` | Get event by ID |
| GET | `/api/events/organizer/{id}` | Get events by organizer |
| POST | `/api/events` | Create new event |
| PUT | `/api/events/{id}` | Update event |
| DELETE | `/api/events/{id}` | Delete event |

### Health & Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/swagger-ui.html` | API documentation |

## 🚀 Running Locally

### Prerequisites

Ensure infrastructure is running:
```bash
cd ../../infrastructure
docker-compose up -d
```

### Start Service
```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or with installed Maven
mvn spring-boot:run
```

### Verify It's Running
```bash
curl http://localhost:8081/actuator/health
```

## 🧪 Testing

### Run Tests
```bash
./mvnw test
```

### Test with Postman
Import the Postman collection from `/postman` directory.

### Test with Swagger
Open http://localhost:8081/swagger-ui.html

## 📊 Database Schema
```sql
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    location VARCHAR(500),
    organizer_id BIGINT NOT NULL,
    max_attendees INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## 📤 Kafka Events Published

- **event-created** - When a new event is created
- **event-updated** - When an event is modified
- **event-deleted** - When an event is deleted

## 🔧 Configuration

Configuration is in `src/main/resources/application.yaml`:

- **Port:** 8081
- **Database:** PostgreSQL (localhost:5432/planify)
- **Kafka:** localhost:9092

## 🐳 Docker

### Build Image
```bash
docker build -t planify/event-manager:latest .
```

### Run Container
```bash
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  planify/event-manager:latest
```

## 📈 Metrics

Prometheus metrics available at `/actuator/prometheus`:
- JVM memory and CPU
- HTTP request rates and latencies
- Database connection pool stats
- Kafka producer/consumer metrics