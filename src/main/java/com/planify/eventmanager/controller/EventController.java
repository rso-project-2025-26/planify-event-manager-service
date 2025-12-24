package com.planify.eventmanager.controller;

import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.service.EventService;
import com.planify.eventmanager.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management operations")
public class EventController {

    private final EventService eventService;
    private final SecurityService securityService;

    // CRUD Operations
    @GetMapping
    @Operation(
            summary = "Get all events",
            description = "Returns a list of all events."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get event by ID",
            description = "Returns an event based on its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    @Operation(
            summary = "Create new event",
            description = "Creates a new event and returns the created object."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid event data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        if (!securityService.hasAnyRoleInOrganization(event.getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to create events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(event));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update event",
            description = "Updates an existing event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Event> updateEvent(
            @PathVariable UUID id,
            @RequestBody Event event
    ) {
        if (!securityService.hasAnyRoleInOrganization(event.getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to update events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(eventService.updateEvent(id, event));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete event",
            description = "Deletes an event by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
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
            summary = "Get events by organization ID",
            description = "Returns all events created by the specified organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    })
    public ResponseEntity<List<Event>> getEventsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(eventService.getEventsByOrganization(organizationId));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get events by status",
            description = "Returns events based on the provided status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<List<Event>> getEventsByStatus(@PathVariable Event.EventStatus status) {
        return ResponseEntity.ok(eventService.getEventsByStatus(status));
    }

    @GetMapping("/public")
    @Operation(
            summary = "Get all public events",
            description = "Returns a list of events marked as public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public events retrieved")
    })
    public ResponseEntity<List<Event>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Get upcoming events",
            description = "Returns all events happening in the future."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upcoming events retrieved")
    })
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/past")
    @Operation(
            summary = "Get past events",
            description = "Returns all events that already occurred."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Past events retrieved")
    })
    public ResponseEntity<List<Event>> getPastEvents() {
        return ResponseEntity.ok(eventService.getPastEvents());
    }

    // Status Management
    @PutMapping("/{id}/publish")
    @Operation(
            summary = "Publish event",
            description = "Changes the event's status to PUBLISHED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event published"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Event> publishEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publishEvent(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel event",
            description = "Changes the event's status to CANCELLED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event cancelled"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Event> cancelEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @PutMapping("/{id}/complete")
    @Operation(
            summary = "Mark event as completed",
            description = "Changes the event's status to COMPLETED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event marked as completed"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Event> completeEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.completeEvent(id));
    }
}