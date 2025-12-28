package com.planify.eventmanager.repository;

import com.planify.eventmanager.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    // Find by organization
    List<Event> findByOrganizationId(UUID organizationId);
    
    // Find by status
    List<Event> findByStatus(Event.EventStatus status);
    
    // Find by event type
    List<Event> findByEventType(Event.EventType eventType);
    
    // Find public events only
    List<Event> findByEventTypeOrderByEventDateAsc(Event.EventType eventType);
    
    // Find events by date range
    List<Event> findByEventDateBetween(LocalDateTime start, LocalDateTime end);
    
    // Find upcoming events (published and not completed)
    @Query("SELECT e FROM Event e WHERE e.eventDate > :now AND e.status = 'PUBLISHED' ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEvents(LocalDateTime now);
    
    // Find past events
    @Query("SELECT e FROM Event e WHERE e.eventDate < :now ORDER BY e.eventDate DESC")
    List<Event> findPastEvents(LocalDateTime now);
    
    // Find events by location
    List<Event> findByLocationId(UUID locationId);
    
    // Find events by organization and status
    List<Event> findByOrganizationIdAndStatus(UUID organizationId, Event.EventStatus status);
}