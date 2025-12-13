package com.planify.eventmanager.service;

import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final EventRepository eventRepository;
    private final KafkaProducer kafkaProducer;
    
    // CRUD Operations    
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }
    
    @Transactional
    public Event createEvent(Event event) {
        Event savedEvent = eventRepository.save(event);
        
        // Publish event to Kafka
        kafkaProducer.sendMessage("event-created", 
            String.format("Event created: %s (ID: %d)", savedEvent.getTitle(), savedEvent.getId()));
        
        log.info("Created new event: {}", savedEvent.getId());
        return savedEvent;
    }
    
    @Transactional
    public Event updateEvent(Long id, Event eventDetails) {
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
            String.format("Event updated: %s (ID: %d)", updatedEvent.getTitle(), updatedEvent.getId()));
        
        log.info("Updated event: {}", updatedEvent.getId());
        return updatedEvent;
    }
    
    @Transactional
    public void deleteEvent(Long id) {
        Event event = getEventById(id);
        eventRepository.delete(event);
        
        // Publish delete event to Kafka
        Map<String, Object> payload = Map.of(
            "eventId", id,
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
    
    public List<Event> getEventsByLocation(Long locationId) {
        return eventRepository.findByLocationId(locationId);
    }
    
    // Status Management    
    @Transactional
    public Event publishEvent(Long id) {
        Event event = getEventById(id);
        event.setStatus(Event.EventStatus.PUBLISHED);
        Event published = eventRepository.save(event);
        
        kafkaProducer.sendMessage("event-published", 
            String.format("Event published: %s (ID: %d)", published.getTitle(), published.getId()));
        
        log.info("Published event: {}", id);
        return published;
    }
    
    @Transactional
    public Event cancelEvent(Long id) {
        Event event = getEventById(id);
        event.setStatus(Event.EventStatus.CANCELLED);
        Event cancelled = eventRepository.save(event);
        
        // Publish cancel event to Kafka
        Map<String, Object> payload = Map.of(
            "eventId", id,
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
    public Event completeEvent(Long id) {
        Event event = getEventById(id);
        event.setStatus(Event.EventStatus.COMPLETED);
        Event completed = eventRepository.save(event);
        
        log.info("Completed event: {}", id);
        return completed;
    }
}