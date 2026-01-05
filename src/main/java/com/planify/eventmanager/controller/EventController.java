package com.planify.eventmanager.controller;

import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.service.EventService;
import com.planify.eventmanager.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event lifecycle management and CRUD operations")
@SecurityRequirement(name = "bearer-jwt")
public class EventController {

    private final EventService eventService;
    private final SecurityService securityService;

    // CRUD Operations
    @GetMapping
    @Operation(
        summary = "Get all events",
        description = "Returns a complete list of all events in the system. Requires ADMINISTRATOR role for system-wide access."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved events",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get event by ID",
        description = "Returns detailed information about a specific event identified by its UUID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved event",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Event> getEventById(
            @Parameter(required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create new event",
        description = "Creates a new event and automatically reserves the location via gRPC if locationId is provided. Publishes 'event-created' Kafka event for downstream processing."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "400", description = "Invalid event data or validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> createEvent(
            @Parameter(required = true)
            @RequestBody Event event) {
        if (!securityService.hasAnyRoleInOrganization(event.getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to create events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Event eventSaved = eventService.createEvent(event);
        eventService.reserveLocation(eventSaved.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventSaved);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update event",
        description = "Updates an existing event. If location changes, automatically updates the reservation via gRPC."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event successfully updated",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "400", description = "Invalid event data or validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not a member of organization or insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> updateEvent(
            @Parameter(required = true)
            @PathVariable UUID id,
            @Parameter(required = true)
            @RequestBody Event event
    ) {
        if (!securityService.hasAnyRoleInOrganization(event.getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to update events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Event eventUpdated = eventService.updateEvent(id, event);
        eventService.reserveLocation(eventUpdated.getId());
        return ResponseEntity.ok(eventUpdated);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete event",
        description = "Permanently deletes an event and cancels any associated location reservations. This action cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Event successfully deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not a member of organization or insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(required = true)
            @PathVariable UUID id) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(id).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // Query Operations
    @GetMapping("/organization/{organizationId}")
    @Operation(
        summary = "Get events by organization",
        description = "Returns all events created and managed by the specified organization."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved organization events",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    public ResponseEntity<List<Event>> getEventsByOrganization(
            @Parameter(required = true)
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(eventService.getEventsByOrganization(organizationId));
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get events by status",
        description = "Returns all events matching the specified status (DRAFT, PUBLISHED, CANCELLED, COMPLETED)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved events",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status value", content = @Content)
    })
    public ResponseEntity<List<Event>> getEventsByStatus(
            @Parameter(required = true)
            @PathVariable Event.EventStatus status) {
        return ResponseEntity.ok(eventService.getEventsByStatus(status));
    }

    @GetMapping("/public")
    @Operation(
        summary = "Get all public events",
        description = "Returns a list of all events marked as public and visible to all users."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved public events",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
    })
    public ResponseEntity<List<Event>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    @GetMapping("/upcoming")
    @Operation(
        summary = "Get upcoming events",
        description = "Returns all events scheduled to occur in the future, sorted by start date ascending."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming events",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
    })
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/past")
    @Operation(
        summary = "Get past events",
        description = "Returns all events that have already occurred, sorted by start date descending."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved past events",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
    })
    public ResponseEntity<List<Event>> getPastEvents() {
        return ResponseEntity.ok(eventService.getPastEvents());
    }

    // Status Management
    @PutMapping("/{id}/publish")
    @Operation(
        summary = "Publish event",
        description = "Changes event status to PUBLISHED, making it visible to guests. Publishes 'event-published' Kafka event and triggers notifications."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event published successfully"),
        @ApiResponse(responseCode = "400", description = "Event not in DRAFT status", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> publishEvent(
        @Parameter(required = true)
        @PathVariable UUID id) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(id).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(eventService.publishEvent(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(
        summary = "Cancel event",
        description = "Changes the event status to CANCELLED. Publishes 'event-cancelled' Kafka event and triggers notification to all invited guests."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event successfully cancelled",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content)
        ,@ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> cancelEvent(
            @Parameter(required = true)
            @PathVariable UUID id) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(id).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @PutMapping("/{id}/complete")
    @Operation(
        summary = "Mark event as completed",
        description = "Changes the event status to COMPLETED after the event has concluded. Used for archiving and analytics purposes."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event marked as completed",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> completeEvent(
            @Parameter(required = true)
            @PathVariable UUID id) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(id).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(eventService.completeEvent(id));
    }
}