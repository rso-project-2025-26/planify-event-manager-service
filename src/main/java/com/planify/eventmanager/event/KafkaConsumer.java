package com.planify.eventmanager.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumer {
    
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @KafkaListener(topics = "event-created", groupId = "${spring.application.name}")
    public void consumeEventCreated(String message) {
        log.info("Consumed message from event-created: {}", message);
    }
    
    @KafkaListener(topics = "event-updated", groupId = "${spring.application.name}")
    public void consumeEventUpdated(String message) {
        log.info("Consumed message from event-updated: {}", message);
    }
    
    @KafkaListener(topics = "event-deleted", groupId = "${spring.application.name}")
    public void consumeEventDeleted(String message) {
        log.info("Consumed message from event-deleted: {}", message);
    }
    
    // Listen to RSVP updates from guest-service
    @KafkaListener(topics = "rsvp-accepted", groupId = "${spring.application.name}")
    @Transactional
    public void consumeRsvpAccepted(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long eventId = json.get("eventId").asLong();
            
            // Increment attendee count
            updateAttendeeCount(eventId, 1);
            
            log.info("RSVP accepted - incremented attendee count for event: {}", eventId);
        } catch (Exception e) {
            log.error("Error processing rsvp-accepted: {}", e.getMessage());
        }
    }
    
    @KafkaListener(topics = "rsvp-declined", groupId = "${spring.application.name}")
    @Transactional
    public void consumeRsvpDeclined(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long eventId = json.get("eventId").asLong();
            boolean wasAccepted = json.has("wasAccepted") && json.get("wasAccepted").asBoolean();
            
            // Only decrement if user previously accepted
            if (wasAccepted) {
                updateAttendeeCount(eventId, -1);
                log.info("RSVP declined (was accepted) - decremented attendee count for event: {}", eventId);
            } else {
                log.info("RSVP declined (was pending) - no change to attendee count for event: {}", eventId);
            }
        } catch (Exception e) {
            log.error("Error processing rsvp-declined: {}", e.getMessage());
        }
    }
    
    // Helper method to update attendee count
    private void updateAttendeeCount(Long eventId, int delta) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            int newCount = Math.max(0, event.getCurrentAttendees() + delta);
            event.setCurrentAttendees(newCount);
            eventRepository.save(event);
            log.info("Updated attendee count for event {}: {} -> {}", 
                eventId, event.getCurrentAttendees() - delta, newCount);
        }
    }
}