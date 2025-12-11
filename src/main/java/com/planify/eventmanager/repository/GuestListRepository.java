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
    
    // Find all events a user is invited to
    List<GuestList> findByUserId(Long userId);
    
    // Find specific guest entry
    Optional<GuestList> findByEventIdAndUserId(Long eventId, Long userId);
    
    // Check if user is invited to event
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    
    // Find by role
    List<GuestList> findByEventIdAndRole(Long eventId, GuestList.GuestRole role);
    
    // Count total guests for event
    Long countByEventId(Long eventId);
    
    // Delete all guests for an event (used when event is deleted)
    void deleteByEventId(Long eventId);
}