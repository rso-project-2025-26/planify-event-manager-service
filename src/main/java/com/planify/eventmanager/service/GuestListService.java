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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestListService {
    
    private final GuestListRepository guestListRepository;
    private final EventRepository eventRepository;
    private final KafkaProducer kafkaProducer;
    
    // Get all guests for an event
    public List<GuestList> getAllGuestsForEvent(Long eventId) {
        return guestListRepository.findByEventId(eventId);
    }
    
    // Get all events user is invited to
    public List<GuestList> getAllEventsForUser(UUID userId) {
        return guestListRepository.findByUserId(userId);
    }
    
    // Get specific guest entry
    public GuestList getGuestEntry(Long eventId, UUID userId) {
        return guestListRepository.findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new RuntimeException("Guest not found for event: " + eventId + " and user: " + userId));
    }
    
    // Invite guest to event (organizer action)
    @Transactional
    public GuestList inviteGuest(Long eventId, UUID userId, UUID invitedByUserId, GuestList.GuestRole role, String notes) {
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
            .role(role != null ? role : GuestList.GuestRole.ATTENDEE)
            .invitedByUserId(invitedByUserId)
            .notes(notes)
            .build();
        
        GuestList saved = guestListRepository.save(guestList);
        
        // Publish Kafka event for guest-service to create invitation
        kafkaProducer.sendMessage("guest-invited", 
            String.format("{\"eventId\": %d, \"userId\": %s, \"invitedBy\": %d, \"invitedAt\": \"%s\"}", 
                eventId, userId, invitedByUserId, LocalDateTime.now()));
        
        log.info("Invited user {} to event {} by user {}", userId, eventId, invitedByUserId);
        return saved;
    }
    
    // Remove guest from event (organizer action)
    @Transactional
    public void removeGuest(Long eventId, UUID userId, UUID removedByUserId) {
        GuestList guest = getGuestEntry(eventId, userId);
        guestListRepository.delete(guest);
        
        // Publish Kafka event for guest-service to remove invitation
        kafkaProducer.sendMessage("guest-removed", 
            String.format("{\"eventId\": %d, \"userId\": %s, \"removedBy\": %d, \"removedAt\": \"%s\"}", 
                eventId, userId, removedByUserId, LocalDateTime.now()));
        
        log.info("Removed user {} from event {} by user {}", userId, eventId, removedByUserId);
    }
    
    // Update guest role (organizer action)
    @Transactional
    public GuestList updateGuestRole(Long eventId, UUID userId, GuestList.GuestRole newRole) {
        GuestList guest = getGuestEntry(eventId, userId);
        guest.setRole(newRole);
        GuestList updated = guestListRepository.save(guest);
        
        log.info("Updated role for user {} in event {} to {}", userId, eventId, newRole);
        return updated;
    }
    
    // Update guest notes (organizer action)
    @Transactional
    public GuestList updateGuestNotes(Long eventId, UUID userId, String notes) {
        GuestList guest = getGuestEntry(eventId, userId);
        guest.setNotes(notes);
        GuestList updated = guestListRepository.save(guest);
        
        log.info("Updated notes for user {} in event {}", userId, eventId);
        return updated;
    }
    
    // Query operations
    public List<GuestList> getGuestsByRole(Long eventId, GuestList.GuestRole role) {
        return guestListRepository.findByEventIdAndRole(eventId, role);
    }
    
    public boolean isUserInvited(Long eventId, UUID userId) {
        return guestListRepository.existsByEventIdAndUserId(eventId, userId);
    }
}