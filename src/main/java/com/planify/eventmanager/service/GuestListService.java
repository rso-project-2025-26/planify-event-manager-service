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

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestListService {
    
    private final GuestListRepository guestListRepository;
    private final EventRepository eventRepository;
    private final KafkaProducer kafkaProducer;
    
    // CRUD Operations    
    public List<GuestList> getAllGuestsForEvent(Long eventId) {
        return guestListRepository.findByEventId(eventId);
    }
    
    public List<GuestList> getAllEventsForUser(Long userId) {
        return guestListRepository.findByUserId(userId);
    }
    
    public GuestList getGuestEntry(Long eventId, Long userId) {
        return guestListRepository.findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new RuntimeException("Guest not found for event: " + eventId + " and user: " + userId));
    }
    
    @Transactional
    public GuestList inviteGuest(Long eventId, Long userId, GuestList.GuestRole role, String notes) {
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
            .rsvpStatus(GuestList.RsvpStatus.PENDING)
            .notes(notes)
            .build();
        
        GuestList saved = guestListRepository.save(guestList);
        
        // Publish invite event to Kafka
        kafkaProducer.sendMessage("guest-invited", 
            String.format("User %d invited to event %d", userId, eventId));
        
        log.info("Invited user {} to event {}", userId, eventId);
        return saved;
    }
    
    @Transactional
    public void removeGuest(Long eventId, Long userId) {
        GuestList guest = getGuestEntry(eventId, userId);
        guestListRepository.delete(guest);
        
        // Publish remove event to Kafka
        kafkaProducer.sendMessage("guest-removed", 
            String.format("User %d removed from event %d", userId, eventId));
        
        log.info("Removed user {} from event {}", userId, eventId);
    }
    
    // RSVP Management    
    @Transactional
    public GuestList updateRsvp(Long eventId, Long userId, GuestList.RsvpStatus status) {
        GuestList guest = getGuestEntry(eventId, userId);
        guest.setRsvpStatus(status);
        guest.setRespondedAt(LocalDateTime.now());
        
        GuestList updated = guestListRepository.save(guest);
        
        // Publish update event to Kafka
        kafkaProducer.sendMessage("rsvp-updated", 
            String.format("User %d RSVP %s for event %d", userId, status, eventId));
        
        log.info("User {} RSVP {} for event {}", userId, status, eventId);
        
        // Update event attendee count
        updateEventAttendeeCount(eventId);
        
        return updated;
    }
    
    @Transactional
    public GuestList acceptInvitation(Long eventId, Long userId) {
        return updateRsvp(eventId, userId, GuestList.RsvpStatus.ACCEPTED);
    }
    
    @Transactional
    public GuestList declineInvitation(Long eventId, Long userId) {
        return updateRsvp(eventId, userId, GuestList.RsvpStatus.DECLINED);
    }
    
    // Check-in Management    
    @Transactional
    public GuestList checkInGuest(Long eventId, Long userId) {
        GuestList guest = getGuestEntry(eventId, userId);
        
        if (!guest.getRsvpStatus().equals(GuestList.RsvpStatus.ACCEPTED)) {
            throw new RuntimeException("Guest has not accepted invitation");
        }
        
        guest.setCheckedIn(true);
        guest.setCheckedInAt(LocalDateTime.now());
        
        GuestList checkedIn = guestListRepository.save(guest);
        
        // Publish check-in event to Kafka
        kafkaProducer.sendMessage("guest-checked-in", 
            String.format("User %d checked in to event %d", userId, eventId));
        
        log.info("User {} checked in to event {}", userId, eventId);
        return checkedIn;
    }
    
    public List<GuestList> getCheckedInGuests(Long eventId) {
        return guestListRepository.findByEventIdAndCheckedIn_True(eventId);
    }
    
    public Long countCheckedInGuests(Long eventId) {
        return guestListRepository.countByEventIdAndCheckedIn_True(eventId);
    }
    
    // Query Operations     
    public List<GuestList> getGuestsByStatus(Long eventId, GuestList.RsvpStatus status) {
        return guestListRepository.findByEventIdAndRsvpStatus(eventId, status);
    }
    
    public List<GuestList> getGuestsByRole(Long eventId, GuestList.GuestRole role) {
        return guestListRepository.findByEventIdAndRole(eventId, role);
    }
    
    public boolean isUserInvited(Long eventId, Long userId) {
        return guestListRepository.existsByEventIdAndUserId(eventId, userId);
    }
    
    // Statistics    
    public Long countTotalGuests(Long eventId) {
        return guestListRepository.countByEventId(eventId);
    }
    
    public Long countGuestsByStatus(Long eventId, GuestList.RsvpStatus status) {
        return guestListRepository.countByEventIdAndRsvpStatus(eventId, status);
    }
    
    // Helper Methods
    private void updateEventAttendeeCount(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            Long acceptedCount = guestListRepository.countByEventIdAndRsvpStatus(eventId, 
                GuestList.RsvpStatus.ACCEPTED);
            event.setCurrentAttendees(acceptedCount.intValue());
            eventRepository.save(event);
        }
    }
}