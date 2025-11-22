package com.planify.eventmanager.repository;

import com.planify.eventmanager.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    List<Event> findByOrganizerId(Long organizerId);
    
    List<Event> findByEventDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<Event> findByStatus(Event.EventStatus status);
}