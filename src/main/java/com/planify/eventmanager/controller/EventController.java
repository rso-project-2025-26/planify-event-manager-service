package com.planify.eventmanager.controller;

import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management operations")
public class EventController {
    
    private final EventService eventService;
    
    // CRUD Operations    
    @GetMapping
    @Operation(summary = "Get all events")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
    
    @PostMapping
    @Operation(summary = "Create new event")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(eventService.createEvent(event));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update event")
    public ResponseEntity<Event> updateEvent(
            @PathVariable Long id, 
            @RequestBody Event event) {
        return ResponseEntity.ok(eventService.updateEvent(id, event));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
    
    // Query Operations    
    @GetMapping("/organizer/{organizerId}")
    @Operation(summary = "Get events by organizer ID")
    public ResponseEntity<List<Event>> getEventsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(organizerId));
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get events by status")
    public ResponseEntity<List<Event>> getEventsByStatus(@PathVariable Event.EventStatus status) {
        return ResponseEntity.ok(eventService.getEventsByStatus(status));
    }
    
    @GetMapping("/public")
    @Operation(summary = "Get all public events")
    public ResponseEntity<List<Event>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }
    
    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming events")
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }
    
    @GetMapping("/past")
    @Operation(summary = "Get past events")
    public ResponseEntity<List<Event>> getPastEvents() {
        return ResponseEntity.ok(eventService.getPastEvents());
    }
    
    @GetMapping("/date-range")
    @Operation(summary = "Get events by date range")
    public ResponseEntity<List<Event>> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(eventService.getEventsByDateRange(start, end));
    }
    
    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get events by location")
    public ResponseEntity<List<Event>> getEventsByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(eventService.getEventsByLocation(locationId));
    }
    
    // Status Management    
    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish event")
    public ResponseEntity<Event> publishEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.publishEvent(id));
    }
    
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel event")
    public ResponseEntity<Event> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }
    
    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark event as completed")
    public ResponseEntity<Event> completeEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.completeEvent(id));
    }
    
    // Statistics    
    @GetMapping("/{id}/is-full")
    @Operation(summary = "Check if event is full")
    public ResponseEntity<Boolean> isEventFull(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.isEventFull(id));
    }
    
    @GetMapping("/organizer/{organizerId}/count")
    @Operation(summary = "Count events by organizer")
    public ResponseEntity<Long> countEventsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(eventService.countEventsByOrganizer(organizerId));
    }
}