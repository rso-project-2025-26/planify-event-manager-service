package com.planify.eventmanager.repository;

import com.planify.eventmanager.model.GuestList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestListRepository extends JpaRepository<GuestList, Long> {
    
    // Find all guests for an event
    List<GuestList> findByEventId(Long eventId);
    
    // Find all events for a user
    List<GuestList> findByUserId(Long userId);
    
    // Find specific guest entry
    Optional<GuestList> findByEventIdAndUserId(Long eventId, Long userId);
    
    // Check if user is invited to event
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    
    // Find by RSVP status
    List<GuestList> findByEventIdAndRsvpStatus(Long eventId, GuestList.RsvpStatus status);
    
    // Count guests by status
    Long countByEventIdAndRsvpStatus(Long eventId, GuestList.RsvpStatus status);
    
    // Count total guests for event
    Long countByEventId(Long eventId);
    
    // Find checked-in guests
    List<GuestList> findByEventIdAndCheckedIn_True(Long eventId);
    
    // Count checked-in guests
    Long countByEventIdAndCheckedIn_True(Long eventId);
    
    // Find NOT checked-in guests
    List<GuestList> findByEventIdAndCheckedIn_False(Long eventId);
    
    // Find by role
    List<GuestList> findByEventIdAndRole(Long eventId, GuestList.GuestRole role);
    
    // Delete all guests for an event
    void deleteByEventId(Long eventId);
}