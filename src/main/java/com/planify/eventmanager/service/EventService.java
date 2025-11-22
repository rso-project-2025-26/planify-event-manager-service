package com.planify.eventmanager.service;

import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final EventRepository eventRepository;
    private final KafkaProducer kafkaProducer;
    
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }
    
    public List<Event> getEventsByOrganizer(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId);
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
        event.setLocation(eventDetails.getLocation());
        event.setMaxAttendees(eventDetails.getMaxAttendees());
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
        kafkaProducer.sendMessage("event-deleted", 
            String.format("Event deleted: ID %d", id));
        
        log.info("Deleted event: {}", id);
    }
}