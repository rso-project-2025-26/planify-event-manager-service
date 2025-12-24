package com.planify.eventmanager.service;

import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.model.GuestList;
import com.planify.eventmanager.repository.EventRepository;
import com.planify.eventmanager.repository.GuestListRepository;
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
public class GuestListService {
    
    private final GuestListRepository guestListRepository;
    private final EventRepository eventRepository;
    private final KafkaProducer kafkaProducer;
    
    // Get all guests for an event
    public List<GuestList> getAllGuestsForEvent(UUID eventId) {
        return guestListRepository.findByEventId(eventId);
    }
    
    // Get all events user is invited to
    public List<GuestList> getAllEventsForUser(UUID userId) {
        return guestListRepository.findByUserId(userId);
    }
    
    // Get specific guest entry
    public GuestList getGuestEntry(UUID eventId, UUID userId) {
        return guestListRepository.findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new RuntimeException("Guest not found for event: " + eventId + " and user: " + userId));
    }
    
    // Invite guest to event (organizer action)
    @Transactional
    public GuestList inviteGuest(UUID eventId, UUID userId, UUID organizationId) {
        // Check if event exists
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        
        // Check if already invited
        if (guestListRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("User already invited to this event");
        }
        
        GuestList guestList = GuestList.builder()
            .eventId(eventId)
            .userId(userId)
            .organizationId(organizationId)
            .build();
        
        GuestList saved = guestListRepository.save(guestList);
        
        // Publish Kafka event for guest-service to create invitation
        Map<String, Object> payload = Map.of(
            "eventId", eventId.toString(),
            "userId", userId.toString(),
            "organizationId", organizationId.toString(),
            "invitedAt", LocalDateTime.now().toString()
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(payload);
            kafkaProducer.sendMessage("guest-invited", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize guest-invited payload for user {} in event {}: {}", userId, eventId, e.getMessage(), e);
        }
        
        log.info("Invited user {} to event {} in organization {}", userId, eventId, organizationId);
        return saved;
    }
    
    // Remove guest from event (organizer action)
    @Transactional
    public void removeGuest(UUID eventId, UUID userId) {
        GuestList guest = getGuestEntry(eventId, userId);
        guestListRepository.delete(guest);
        
        // Publish Kafka event for guest-service to remove invitation
        Map<String, Object> payload = Map.of(
            "eventId", eventId.toString(),
            "userId", userId.toString(),
            "removedAt", LocalDateTime.now().toString()
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(payload);
            kafkaProducer.sendMessage("guest-removed", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize guest-removed payload for user {} in event {}: {}", userId, eventId, e.getMessage(), e);
        }
        
        log.info("Removed user {} from event {}", userId, eventId);
    }
    
    // Query operations
    public boolean isUserInvited(UUID eventId, UUID userId) {
        return guestListRepository.existsByEventIdAndUserId(eventId, userId);
    }
    
    public List<GuestList> getGuestsByOrganization(UUID organizationId) {
        return guestListRepository.findByOrganizationId(organizationId);
    }
}