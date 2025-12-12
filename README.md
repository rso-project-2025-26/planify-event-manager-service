# Event Manager Service

Core microservice for event organizers to create, manage, and orchestrate events.

## Purpose

Handles all organizer-facing event management:
- Create and manage events (CRUD operations)
- Manage guest lists (invite/remove guests)
- Track event status lifecycle (draft, published, cancelled, completed)
- Monitor attendee counts in real-time
- Coordinate with guest-service via Kafka messaging

## Architecture

- **Port**: 8081
- **Database**: PostgreSQL (schema: event_manager)
- **Messaging**: Kafka consumer & producer
- **API Documentation**: Swagger UI at http://localhost:8081/swagger-ui.html

## Database Schema

### events table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| title | VARCHAR(255) | Event name |
| description | TEXT | Event description |
| event_date | TIMESTAMP | Event start date/time |
| end_date | TIMESTAMP | Event end date/time |
| location_id | BIGINT | Reference to booking-service location |
| location_name | VARCHAR(500) | Location name/address |
| organization_id | UUID | Organization that owns the event |
| organizer_id | UUID | User who created the event |
| max_attendees | INTEGER | Maximum capacity |
| current_attendees | INTEGER | Current accepted count (updated via Kafka) |
| event_type | VARCHAR(50) | PUBLIC or PRIVATE |
| status | VARCHAR(50) | DRAFT, PUBLISHED, CANCELLED, COMPLETED |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### guest_list table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| event_id | BIGINT | Foreign key to events |
| user_id | UUID | Reference to user in user-service |
| role | VARCHAR(50) | ATTENDEE, SPEAKER, VIP, STAFF |
| notes | TEXT | Organizer notes about guest |
| invited_by_user_id | UUID | User who sent invitation |
| invited_at | TIMESTAMP | When invitation was sent |

**Constraints:**
- UNIQUE(event_id, user_id) - one invitation per guest per event
- CASCADE DELETE on event deletion

## API Endpoints

### Event Management
| Method | Endpoint | Body / Params | Description |
|--------|----------|----------------|-------------|
| GET | `/api/events` | — | Get all events |
| GET | `/api/events/{id}` | Path: `id` | Get event by ID |
| POST | `/api/events` | JSON body | Create new event |
| PUT | `/api/events/{id}` | Path: `id` + JSON body | Update event |
| DELETE | `/api/events/{id}` | Path: `id` | Delete event |

---

### Event Query Operations
| Method | Endpoint | Params | Description |
|--------|----------|---------|-------------|
| GET | `/api/events/organization/{organizationId}` | Path: `organizationId` | Get events belonging to an organization |
| GET | `/api/events/status/{status}` | Path: `status` | Get events by status (DRAFT, PUBLISHED, CANCELLED, COMPLETED) |
| GET | `/api/events/public` | — | Get all PUBLIC events |
| GET | `/api/events/upcoming` | — | Get upcoming future events |
| GET | `/api/events/past` | — | Get past events |

---

### Event Status Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/events/{id}/publish` | Change status to PUBLISHED |
| PUT | `/api/events/{id}/cancel` | Change status to CANCELLED |
| PUT | `/api/events/{id}/complete` | Mark event as COMPLETED |

---

### Guest List Management
| Method | Endpoint | Params / Body | Description |
|--------|----------|----------------|-------------|
| GET | `/api/events/{eventId}/guests` | Path: `eventId` | Get all guests for event |
| GET | `/api/events/{eventId}/guests/{userId}` | Path: `eventId`, `userId` | Get specific guest |
| POST | `/api/events/{eventId}/guests/invite` | Query: `userId`, `invitedByUserId`, `role`, `notes` | Invite a guest |
| DELETE | `/api/events/{eventId}/guests/{userId}` | Query: `removedByUserId` | Remove a guest |
| PUT | `/api/events/{eventId}/guests/{userId}/role` | Query: `role` | Update guest role |
| PUT | `/api/events/{eventId}/guests/{userId}/notes` | Body: text | Update guest's notes |
| GET | `/api/events/{eventId}/guests/role/{role}` | Path: `eventId`, `role` | Get guests by role |

---

### Kafka — Published Topics
| Topic | Trigger | Payload Summary |
|--------|---------|-----------------|
| `event-created` | Event created | Event summary |
| `event-updated` | Event updated | Event summary |
| `event-deleted` | Event deleted | `{ eventId, deletedAt }` |
| `event-published` | Event published | Summary |
| `event-cancelled` | Event cancelled | `{ eventId, cancelledAt }` |
| `guest-invited` | Guest invited | `{ eventId, userId, invitedBy, invitedAt }` |
| `guest-removed` | Guest removed | `{ eventId, userId, removedBy, removedAt }` |

---

### Kafka — Consumed Topics
| Topic | From | Action |
|--------|------|--------|
| `rsvp-accepted` | guest-service | Increment attendee count |
| `rsvp-declined` | guest-service | Decrement attendee count (if previously accepted) |

### Integration with Other Services

### user-service
- Uses UUID for organizationId and userId references
- Organizations own events (organizationId)
- Users create events (organizerId)
- No direct API calls - references only

### guest-service
- Two-way Kafka communication
- event-manager publishes: guest-invited, guest-removed, event-deleted
- guest-service publishes: rsvp-accepted, rsvp-declined
- event-manager updates current_attendees based on RSVP events

### booking-service (future)
- Will reference locationId for venue bookings
- Integration via REST/gRPC

### notification-service (future)
- Consumes event-published, event-cancelled
- Sends notifications to guests

## Running Locally

1. Start infrastructure:
```bash
cd infrastructure
docker compose up -d
```

2. Run the service:
```bash
./mvnw spring-boot:run
```

3. Access Swagger UI:
```
http://localhost:8081/swagger-ui.html
```

## Configuration

Key application properties:
- `server.port`: 8081
- `spring.datasource.url`: jdbc:postgresql://localhost:5432/planify
- `spring.jpa.properties.hibernate.default_schema`: event_manager
- `spring.kafka.bootstrap-servers`: localhost:9092
- `spring.kafka.consumer.group-id`: event-manager-service

## Business Logic

### Event Status Lifecycle
```
DRAFT → PUBLISHED → COMPLETED
  ↓
CANCELLED (from any status)
```

### Attendee Count Management
- Starts at 0 when event created
- Auto-increments when guest accepts (via Kafka)
- Auto-decrements when guest declines after accepting
- Never goes below 0
- Compared against max_attendees for capacity checks

### Guest List Rules
- One invitation per user per event (unique constraint)
- Deleting event cascades to guest_list
- Removing guest publishes Kafka event for guest-service sync

## Multi-Tenancy

Events are scoped to organizations:
- `organizationId`: Which organization owns the event
- `organizerId`: Which user created it (for audit trail)

Users must have appropriate role in organization to:
- Create events (ORGANISER or ORG_ADMIN)
- Manage guest lists (ORGANISER or ORG_ADMIN)
- Publish/cancel events (ORG_ADMIN)