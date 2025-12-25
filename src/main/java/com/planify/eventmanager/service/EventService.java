package com.planify.eventmanager.service;

import com.planify.booking_service.grpc.CancelBookingResponse;
import com.planify.eventmanager.booking.BookingClient;
import com.planify.booking_service.grpc.CheckAvailabilityResponse;
import com.planify.booking_service.grpc.CreateBookingResponse;
import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final EventRepository eventRepository;
    private final KafkaProducer kafkaProducer;
    private final BookingClient bookingClient;

    // CRUD Operations    
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    
    public Event getEventById(UUID id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }
    
    @Transactional
    public Event createEvent(Event event) {
        Event savedEvent = eventRepository.save(event);
        
        // Publish event to Kafka
        kafkaProducer.sendMessage("event-created", 
            String.format("Event created: %s (ID: %s)", savedEvent.getTitle(), savedEvent.getId()));
        
        log.info("Created new event: {}", savedEvent.getId());
        return savedEvent;
    }
    
    @Transactional
    public Event updateEvent(UUID id, Event eventDetails) {
        Event event = getEventById(id);
        
        event.setTitle(eventDetails.getTitle());
        event.setDescription(eventDetails.getDescription());
        event.setEventDate(eventDetails.getEventDate());
        event.setEndDate(eventDetails.getEndDate());
        event.setLocationId(eventDetails.getLocationId());
        event.setLocationName(eventDetails.getLocationName());
        event.setMaxAttendees(eventDetails.getMaxAttendees());
        event.setEventType(eventDetails.getEventType());
        event.setStatus(eventDetails.getStatus());
        
        Event updatedEvent = eventRepository.save(event);
        
        // Publish update event to Kafka
        kafkaProducer.sendMessage("event-updated", 
            String.format("Event updated: %s (ID: %s)", updatedEvent.getTitle(), updatedEvent.getId()));
        
        log.info("Updated event: {}", updatedEvent.getId());
        return updatedEvent;
    }
    
    @Transactional
    public void deleteEvent(UUID id) {
        Event event = getEventById(id);

        if (event.getBookingId() != null) {
            try {
                log.info("Canceling booking {} for event {}", event.getBookingId(), id);
                var cancel = bookingClient.cancelBooking(event.getBookingId());
                event.setBookingStatus(cancel.getStatus());
            } catch (Exception ex) {
                log.error("Failed to cancel booking {} for event {}: {}", event.getBookingId(), id, ex.getMessage());
            }
        }

        eventRepository.delete(event);
        
        // Publish delete event to Kafka
        Map<String, Object> payload = Map.of(
            "eventId", id.toString(),
            "deletedAt", LocalDateTime.now().toString()
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(payload);
            kafkaProducer.sendMessage("event-deleted", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event-deleted payload for event {}: {}", id, e.getMessage(), e);
        }
        
        log.info("Deleted event: {}", id);
    }
    
    // Query Operations    
    public List<Event> getEventsByOrganization(UUID organizationId) {
        return eventRepository.findByOrganizationId(organizationId);
    }
    
    public List<Event> getEventsByStatus(Event.EventStatus status) {
        return eventRepository.findByStatus(status);
    }
    
    public List<Event> getPublicEvents() {
        return eventRepository.findByEventTypeOrderByEventDateAsc(Event.EventType.PUBLIC);
    }
    
    public List<Event> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now());
    }
    
    public List<Event> getPastEvents() {
        return eventRepository.findPastEvents(LocalDateTime.now());
    }
    
    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByEventDateBetween(start, end);
    }
    
    public List<Event> getEventsByLocation(UUID locationId) {
        return eventRepository.findByLocationId(locationId);
    }
    
    // Reservira lokacijo
    @Transactional
    public void reserveLocation(UUID id) {
        Event event = getEventById(id);

        if (event.getBookingId() != null) {
            log.info("Canceling existing booking for event {}", id);
            CancelBookingResponse cancel = bookingClient.cancelBooking(event.getBookingId());
        }

        if (event.getLocationId() != null && event.getEventDate() != null && event.getEndDate() != null) {
            long startMs = event.getEventDate().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
            long endMs = event.getEndDate().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();

            CheckAvailabilityResponse availability = bookingClient.checkAvailability(
                    event.getLocationId(), startMs, endMs);
            if (!availability.getAvailable()) {
                log.warn("Location is not available for the selected time interval");
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Location is not available for the selected time interval");
            }

            CreateBookingResponse create = bookingClient.createBooking(
                    event.getLocationId(),
                    event.getId(),
                    event.getOrganizationId().toString(),
                    startMs,
                    endMs,
                    "EUR",
                    null
            );

            event.setBookingId(UUID.fromString(create.getBookingId()));
            event.setBookingStatus(create.getStatus());

            eventRepository.save(event);
            log.info("Created booking for event {}", id);
        }
    }

    @Transactional
    public Event publishEvent(UUID id) {
        Event event = getEventById(id);
        event.setStatus(Event.EventStatus.PUBLISHED);
        Event published = eventRepository.save(event);
        
        kafkaProducer.sendMessage("event-published", 
            String.format("Event published: %s (ID: %s)", published.getTitle(), published.getId()));
        
        log.info("Published event: {}", id);
        return published;
    }
    
    @Transactional
    public Event cancelEvent(UUID id) {
        Event event = getEventById(id);

        // If there is a booking, cancel it first
        if (event.getBookingId() != null) {
            try {
                log.info("Canceling booking {} for event {}", event.getBookingId(), id);
                var cancel = bookingClient.cancelBooking(event.getBookingId());
                event.setBookingStatus(cancel.getStatus());
            } catch (Exception ex) {
                log.error("Failed to cancel booking {} for event {}: {}", event.getBookingId(), id, ex.getMessage());
                // proceed with event cancel to avoid blocking organizer
            }
        }

        event.setStatus(Event.EventStatus.CANCELLED);
        Event cancelled = eventRepository.save(event);
        
        // Publish cancel event to Kafka
        Map<String, Object> payload = Map.of(
            "eventId", id.toString(),
            "cancelledAt", LocalDateTime.now().toString()
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(payload);
            kafkaProducer.sendMessage("event-cancelled", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event-cancelled payload for event {}: {}", id, e.getMessage(), e);
        }
        
        log.info("Cancelled event: {}", id);
        return cancelled;
    }
    
    @Transactional
    public Event completeEvent(UUID id) {
        Event event = getEventById(id);
        event.setStatus(Event.EventStatus.COMPLETED);
        Event completed = eventRepository.save(event);
        
        log.info("Completed event: {}", id);
        return completed;
    }
}