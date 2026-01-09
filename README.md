# Event Manager Service

Microservice for managing events and guest lists in the Planify platform. Provides RESTful APIs secured with Keycloak authentication, integrates with booking-service via gRPC for location reservations, and publishes events via Kafka.

## Technologies

### Backend Framework & Language
- **Java 21** - Programming language
- **Spring Boot 3.5.7** - Application framework
- **Spring Security** - Security and authentication
- **Spring Data JPA** - Database access
- **Hibernate** - ORM framework
- **Lombok** - Boilerplate code reduction

### Database
- **PostgreSQL** - Database
- **Flyway** - Database migrations
- **HikariCP** - Connection pooling

### Security & Authentication
- **Keycloak** - OAuth2/OIDC authentication and authorization
- **Spring OAuth2 Resource Server** - JWT validation

### Messaging System
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration

### gRPC Communication
- **gRPC Client** - Communication with booking-service
- **Protobuf** - Serialization format
- **grpc-spring-boot-starter** - Spring Boot gRPC integration

### Monitoring & Health
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer Prometheus** - Metrics export
- **Resilience4j** - Circuit breakers, retry, rate limiting, bulkheads

### API Documentation
- **SpringDoc OpenAPI 3** - OpenAPI/Swagger documentation

### Containerization
- **Docker** - Application containerization
- **Kubernetes/Helm** - Orchestration (Helm charts included)

## System Integrations

- **Keycloak**: OAuth2/OIDC authentication and authorization. All endpoints require a valid JWT Bearer token, except public event queries.
- **Kafka**: Publishes domain events for event lifecycle (creation, updates, guest invitations) consumed by notification-service, analytics-service, and guest-service.
- **PostgreSQL**: Stores all event and guest list data via Hibernate/JPA with Flyway migrations in the `event_manager` schema.
- **booking-service (gRPC)**: Synchronous communication for checking location availability and creating reservations.
- **user-service**: Validates user permissions and organization membership via JWT claims.
- **guest-service**: Receives event updates and manages RSVP from guest perspective.

## Roles

### Keycloak Roles

Application-wide roles managed by Keycloak and enforced in this service:

- **UPORABNIK** — Standard authenticated user (can view events they're invited to)
- **ORGANISER** — Can create and manage events within their organization
- **ORG_ADMIN** — Full organization admin permissions + organiser capabilities
- **ADMINISTRATOR** — System administrator with access to all events

## API Endpoints

All endpoints require `Authorization: Bearer <JWT_TOKEN>` header unless otherwise specified.

### Events (`/api/events`)

- `GET /api/events` — List all events (ADMINISTRATOR only)
- `GET /api/events/{id}` — Get event details by ID
- `POST /api/events` — Create new event (ORG_ADMIN or ORGANISER)
- `PUT /api/events/{id}` — Update event (ORG_ADMIN or ORGANISER)
- `DELETE /api/events/{id}` — Delete event (ORG_ADMIN or ORGANISER)

### Query Operations

- `GET /api/events/organization/{organizationId}` — Get events by organization
- `GET /api/events/status/{status}` — Get events by status (DRAFT, PUBLISHED, CANCELLED, COMPLETED)
- `GET /api/events/public` — Get all public events (no auth required)
- `GET /api/events/upcoming` — Get upcoming events
- `GET /api/events/past` — Get past events

### Status Management

- `PUT /api/events/{id}/publish` — Change event status to PUBLISHED
- `PUT /api/events/{id}/cancel` — Change event status to CANCELLED
- `PUT /api/events/{id}/complete` — Change event status to COMPLETED

### Guest List Management (`/api/events/{eventId}/guests`)

- `GET /api/events/{eventId}/guests` — Get all guests for an event
- `GET /api/events/{eventId}/guests/{userId}` — Get specific guest entry
- `POST /api/events/{eventId}/guests/invite?userId={userId}&organizationId={orgId}` — Invite guest to event
- `DELETE /api/events/{eventId}/guests/{userId}` — Remove guest from event

### Minimal curl examples

```bash
# Get event by ID
curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8081/api/events/550e8400-e29b-41d4-a716-446655440000"

# Create new event
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "title": "Annual Conference 2024",
       "description": "Company annual conference",
       "eventDate": "2024-12-15T10:00:00",
       "endDate": "2024-12-15T18:00:00",
       "locationId": "660e8400-e29b-41d4-a716-446655440001",
       "organizationId": "880e8400-e29b-41d4-a716-446655440003",
       "organizerId": "990e8400-e29b-41d4-a716-446655440004",
       "maxAttendees": 100,
       "eventType": "PRIVATE"
     }' \
     "http://localhost:8081/api/events"

# Invite guest to event
curl -X POST -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8081/api/events/550e8400-e29b-41d4-a716-446655440000/guests/invite?userId=990e8400-e29b-41d4-a716-446655440004&organizationId=880e8400-e29b-41d4-a716-446655440003"

# Publish event
curl -X PUT -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8081/api/events/550e8400-e29b-41d4-a716-446655440000/publish"
```

## Database Structure

The service uses PostgreSQL with the following core entities in the `event_manager` schema:

### Events

Core event records managed by organizers. Contains:

- `id` (UUID, PK)
- `title` (VARCHAR) - Event title
- `description` (TEXT) - Event description
- `event_date` (TIMESTAMP) - Event start date/time
- `end_date` (TIMESTAMP) - Event end date/time
- `location_id` (UUID) - Reference to location in booking-service
- `location_name` (VARCHAR) - Cached location name
- `booking_id` (UUID) - Reference to booking in booking-service
- `booking_status` (VARCHAR) - Cached booking status
- `organizer_id` (UUID) - Reference to organizer in user-service
- `organization_id` (UUID) - Reference to organization in user-service
- `max_attendees` (INT) - Maximum number of attendees
- `current_attendees` (INT) - Current number of accepted guests
- `event_type` (VARCHAR) - Event visibility: `PUBLIC`, `PRIVATE`
- `status` (VARCHAR) - Event status: `DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED`
- `created_at` (TIMESTAMP) - Creation timestamp
- `updated_at` (TIMESTAMP) - Last update timestamp

**Indexes:**
- `idx_events_organization` on `organization_id`
- `idx_events_date` on `event_date`
- `idx_events_status` on `status`
- `idx_events_type` on `event_type`
- `idx_events_location` on `location_id`

### Guest List

Tracks guests invited to events (organizer perspective). Contains:

- `id` (UUID, PK)
- `event_id` (UUID, FK to events)
- `user_id` (UUID) - Reference to user in user-service
- `organization_id` (UUID) - Organization ID for permission checks
- `invited_at` (TIMESTAMP) - Invitation timestamp

**Indexes:**
- `idx_guest_list_event` on `event_id`
- `idx_guest_list_user` on `user_id`
- `idx_guest_list_organization` on `organization_id`

**Constraints:**
- Unique constraint on `(event_id, user_id)` - prevents duplicate invitations
- Foreign key to `events(id)` with `ON DELETE CASCADE`

**Relationships**: All entities use UUIDs for cross-service references without foreign key constraints (except within the same schema). Audit fields (`created_at`, `updated_at`) track changes. Database schema is versioned via Flyway migrations in `src/main/resources/db/migration/`.

**Note**: RSVP status is tracked in the `guest-service` schema. This service only tracks who was invited from the organizer's perspective.

## Installation and Setup

### Prerequisites

- Java 21 or newer
- Maven 3.6+
- Docker and Docker Compose
- Git

### Infrastructure Setup

This service requires PostgreSQL, Kafka, Keycloak, and booking-service to run. These dependencies are provided via Docker containers in the main Planify repository.

Clone and setup the infrastructure:

```bash
# Clone the main Planify repository
git clone https://github.com/rso-project-2025-26/planify.git
cd planify

# Follow the setup instructions in the main repository README
# This will start all required infrastructure services (PostgreSQL, Kafka, Keycloak)
```

Refer to the main Planify repository (https://github.com/rso-project-2025-26/planify) documentation for detailed infrastructure setup instructions.

### Configuration

The application uses a single `application.yaml` configuration file located in `src/main/resources/`.

Important environment variables:

```
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/planify
SPRING_DATASOURCE_USERNAME=planify
SPRING_DATASOURCE_PASSWORD=planify
OAUTH2_ISSUER_URI=http://localhost:9080/realms/planify
OAUTH2_JWK_SET_URI=http://localhost:9080/realms/planify/protocol/openid-connect/certs
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_EVENT_ATTENDANCE_ACCEPTED=event-attendance-accepted
BOOKING_GRPC_HOST=localhost
BOOKING_GRPC_PORT=9095
```

### Local Run

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run
```

### Using Makefile

```bash
# Build project
make build

# Docker build
make docker-build

# Docker run
make docker-run

# Tests
make test
```

### Docker Run

```bash
# Build Docker image
docker build -t planify/event-manager-service:0.0.1 .

# Run container
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e OAUTH2_ISSUER_URI=http://host.docker.internal:9080/realms/planify \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e BOOKING_GRPC_HOST=host.docker.internal \
  planify/event-manager-service:0.0.1
```

### Kubernetes/Helm Deployment

```bash
# Install with Helm
helm install event-manager-service ./helm/event-manager

# Install with specific environment values
helm install event-manager-service ./helm/event-manager -f ./helm/event-manager/values-dev.yaml

# Upgrade
helm upgrade event-manager-service ./helm/event-manager

# Uninstall
helm uninstall event-manager-service
```

### Flyway Migrations

Migrations are located in `src/main/resources/db/migration/`:

- `V1__init.sql` - Initial schema with events and guest_list tables
- `V2__add_booking_link.sql` - Add booking reference fields to events table

Manual migration run:

```bash
mvn flyway:migrate
```

## Health Check & Monitoring

### Actuator Endpoints

- **GET** `/actuator/health` — Health check endpoint
- **GET** `/actuator/health/liveness` — Liveness probe
- **GET** `/actuator/health/readiness` — Readiness probe
- **GET** `/actuator/prometheus` — Prometheus metrics
- **GET** `/actuator/info` — Application information
- **GET** `/actuator/metrics` — Application metrics

### API Documentation

After starting the application, Swagger UI is available at:
```
http://localhost:8081/swagger-ui.html
```

OpenAPI specification:
```
http://localhost:8081/api-docs
```

## Kafka Events

The service publishes the following events to Kafka:

### Event Lifecycle Events

**event-attendance-accepted** — Published when a guest accepts an event invitation (consumed from guest-service)

Contains:
- `eventId` - UUID of the event
- `userId` - UUID of the user who accepted
- `organizationId` - UUID of the organization
- `timestamp` - Event timestamp

Additional events published (topics may vary):
- Event created
- Event updated
- Event cancelled
- Guest invited
- Guest removed

These events are consumed by:
- **notification-service** - Sends email/SMS notifications to guests
- **analytics-service** - Collects metrics and generates reports
- **guest-service** - Updates RSVP status and invitation records

## gRPC Integration

### booking-service Communication

The service uses gRPC to communicate with booking-service for location reservations:

**Operations:**
- `CheckAvailability` - Verifies if a location is available for the event time slot
- `CreateBooking` - Creates a reservation for the event location
- `CancelBooking` - Cancels a location reservation

**Configuration:**
```yaml
booking:
  grpc:
    host: localhost
    port: 9095
```

**Circuit Breaker Protection:**
All gRPC calls are protected by Resilience4j circuit breakers to prevent cascading failures.

## Resilience4j

The service implements:

- **Circuit Breakers** - Prevention of cascading failures for:
  - `bookingGrpcService` - Protects gRPC calls to booking-service
  - `defaultCircuitBreaker` - General protection for other operations
- **Retry** - Automatic retry of failed calls with exponential backoff
- **Rate Limiting** - Request rate limiting to prevent overload
- **Bulkheads** - Resource isolation for concurrent operations
- **Time Limiters** - Timeout protection for long-running operations

Configuration is managed via `application.yaml` with health indicators exposed through Actuator.

**Example Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      bookingGrpcService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=EventServiceTest

# Run with coverage report
mvn test jacoco:report
```

Tests are located in `src/test/java/com/planify/eventmanager/` and include:

- `EventServiceTest` - Event CRUD and business logic
- `GuestListServiceTest` - Guest invitation and management