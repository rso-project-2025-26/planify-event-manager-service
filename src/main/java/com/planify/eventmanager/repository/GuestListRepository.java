package com.planify.eventmanager.repository;

import com.planify.eventmanager.model.GuestList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestListRepository extends JpaRepository<GuestList, Long> {
    
    // Find all guests for an event
    List<GuestList> findByEventId(Long eventId);
    
    // Find all events a user is invited to
    List<GuestList> findByUserId(UUID userId);
    
    // Find specific guest entry
    Optional<GuestList> findByEventIdAndUserId(Long eventId, UUID userId);
    
    // Check if user is invited to event
    boolean existsByEventIdAndUserId(Long eventId, UUID userId);
    
    // Find by role
    List<GuestList> findByEventIdAndRole(Long eventId, GuestList.GuestRole role);
    
    // Delete all guests for an event (used when event is deleted)
    void deleteByEventId(Long eventId);
}