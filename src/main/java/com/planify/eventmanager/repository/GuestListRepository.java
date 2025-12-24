package com.planify.eventmanager.repository;

import com.planify.eventmanager.model.GuestList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestListRepository extends JpaRepository<GuestList, UUID> {
    
    // Find all guests for an event
    List<GuestList> findByEventId(UUID eventId);
    
    // Find all events a user is invited to
    List<GuestList> findByUserId(UUID userId);
    
    // Find by organization
    List<GuestList> findByOrganizationId(UUID organizationId);
    
    // Find specific guest entry
    Optional<GuestList> findByEventIdAndUserId(UUID eventId, UUID userId);
    
    // Check if user is invited to event
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
    
    // Delete all guests for an event (used when event is deleted)
    void deleteByEventId(UUID eventId);
}